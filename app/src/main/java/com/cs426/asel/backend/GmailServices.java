package com.cs426.asel.backend;

import static java.lang.Math.min;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cs426.asel.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.android.gms.common.api.Scope;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GmailServices {
    private final Context context;
    private ExecutorService executorService;
    private final EmailCallback emailCallback;
    private String mCurrentPageToken;

    public GmailServices(Context context, EmailCallback emailCallback) {
        this.context = context;
        this.emailCallback = emailCallback;
        mCurrentPageToken = null;
    }

    private Gmail getGmailService(GoogleSignInAccount account) {
        GoogleSignInAccount lastSignedIn = GoogleSignIn.getLastSignedInAccount(context);
        if (lastSignedIn == null) {
            return null;
        }
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(GmailScopes.GMAIL_READONLY));
        credential.setSelectedAccount(account.getAccount());
        return new Gmail.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("ASEL")
                .build();
    }

    public void fetchNextIdBatch(long maxSize) {
        Log.d("GmailServices", "Start fetching next batch of emails");
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            List<Message> emailIDs = new ArrayList<>();
            try {
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
                if (account == null) {
                    Log.d("GmailServices", "Account is null. Skipping.");
                    return;
                }

                Gmail gmailService = getGmailService(account);
                String user = "me";

                do {
                    assert gmailService != null;
                    ListMessagesResponse messagesResponse = gmailService.users().messages().list(user)
                            .setMaxResults(min(maxSize, 500))  // You can increase this value up to 500
                            .setPageToken(mCurrentPageToken)
                            .execute();

                    List<Message> messageList = messagesResponse.getMessages();
                    if (messageList != null) {
                        emailIDs.addAll(messageList);
                    }

                    // Get the nextPageToken to continue fetching emails on later calls
                    mCurrentPageToken = messagesResponse.getNextPageToken();
                    Log.d("GmailServices", "Fetched " + emailIDs.size() + " emails so far.");
                } while (mCurrentPageToken != null && emailIDs.size() < maxSize);

                Log.d("GmailServices", "Fetched " + emailIDs.size() + " emails in total.");
                emailCallback.onEmailIDsFetched(emailIDs);

            } catch (GoogleJsonResponseException e) {
                Log.e("GmailServices", "Google API error: " + e.getMessage(), e);
                emailCallback.onEmailIDsFetched(null);
            } catch (IOException e) {
                Log.e("GmailServices", "Network error: " + e.getMessage(), e);
                emailCallback.onEmailIDsFetched(null);
            } catch (Exception e) {
                Log.e("GmailServices", "Unknown error occurred", e);
                emailCallback.onEmailIDsFetched(null);
            }
        });
    }

    public void fetchAllEmailIDs() {
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            List<Message> emailIDs = new ArrayList<>();
            try {
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
                if (account == null) {
                    Log.d("GmailServices", "Account is null. Skipping.");
                    return;
                }

                Gmail gmailService = getGmailService(account);
                String user = "me";
                String pageToken = null;

                do {
                    ListMessagesResponse messagesResponse = gmailService.users().messages().list(user)
                            .setMaxResults(100L)  // You can increase this value up to 500
                            .setPageToken(pageToken)
                            .execute();

                    List<Message> messageList = messagesResponse.getMessages();
                    if (messageList != null) {
                        emailIDs.addAll(messageList);
                    }

                    // Get the nextPageToken to continue fetching emails
                    pageToken = messagesResponse.getNextPageToken();

                    Log.d("GmailServices", "Fetched " + emailIDs.size() + " emails so far.");

                } while (pageToken != null);

                Log.d("GmailServices", "Fetched " + emailIDs.size() + " emails in total.");

                emailCallback.onEmailIDsFetched(emailIDs);

            } catch (GoogleJsonResponseException e) {
                Log.e("GmailServices", "Google API error: " + e.getMessage(), e);
                emailCallback.onEmailIDsFetched(null);
            } catch (IOException e) {
                Log.e("GmailServices", "Network error: " + e.getMessage(), e);
                emailCallback.onEmailIDsFetched(null);
            } catch (Exception e) {
                Log.e("GmailServices", "Unknown error occurred", e);
                emailCallback.onEmailIDsFetched(null);
            }
        });
    }

    public void fetchEmailByIds(List<String> ids, FetchEmailCallback callback) {
        // Create a fixed thread pool with 4 threads
        ExecutorService executorService = Executors.newFixedThreadPool(16);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        executorService.submit(() -> {
            try {
                List<Message> messages = new ArrayList<>();
                Gmail gmailService = getGmailService(account);

                // Create a list to hold futures for asynchronous email fetching
                List<Future<Message>> futures = new ArrayList<>();

                // Submit tasks to fetch emails concurrently
                for (String id : ids) {
                    futures.add(executorService.submit(() -> {
                        Message message = gmailService.users().messages().get("me", id).execute();
                        return message;
                    }));
                }

                // Collect results from the futures
                for (Future<Message> future : futures) {
                    try {
                        messages.add(future.get()); // Blocking call to get the result
                    } catch (InterruptedException | ExecutionException e) {
                        Log.e("GmailServices", "Error fetching email: " + e.getMessage());
                    }
                }

                callback.onEmailFetched(messages);
            } catch (Exception e) {
                Log.e("GmailServices", "Error: " + e.getMessage());
                callback.onFetchEmailFailed("Error: " + e.getMessage());
            }
        });

    }

    public interface FetchEmailCallback {
        void onEmailFetched(List<Message> message);
        void onFetchEmailFailed(String errorMessage);
    }

    public interface SignInCallback {
        void onSignInSuccess(GoogleSignInAccount account);
        void onSignInFailure(String errorMessage);
    }

    public interface EmailCallback {
        void onEmailIDsFetched(List<Message> emailIDs);
        void onEmailContentFetched();
    }
}
