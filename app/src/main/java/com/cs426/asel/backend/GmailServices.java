package com.cs426.asel.backend;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GmailServices {
    private Context context;
    private ExecutorService executorService;
    private EmailCallback emailCallback;

    public GmailServices(Context context, EmailCallback emailCallback) {
        this.context = context;
        this.emailCallback = emailCallback;
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

    public void fetchAllEmailIDs() {
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            List<Message> emailIDs = null;
            try {
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
                if (account == null) {
                    Log.d("GmailServices", "Account is null. Skipping.");
                }
                else {
                    Gmail gmailService = getGmailService(account);
                    ListMessagesResponse messagesResponse = gmailService.users().messages().list("me").execute();
                    List<Message> messageList = messagesResponse.getMessages();
                    Log.d("GmailServices", "Fetched " + (messageList != null ? messageList.size() : 0) + " emails.");

                    if (messageList != null) {
                        emailIDs = messageList;
                    }
                }
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
                        Log.d("GmailServices", "Fetched email with ID: " + id);
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
