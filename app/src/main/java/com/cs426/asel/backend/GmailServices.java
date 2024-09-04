package com.cs426.asel.backend;

import static android.provider.Settings.System.getString;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GmailServices {
    private Context context;
    private Gmail gmailService;
    private GoogleSignInClient googleSignInClient;
    private ExecutorService executorService;
    private ActivityResultLauncher<Intent> signInLauncher;
    private SignInCallback signInCallback;

    public GmailServices(Context context, ActivityResultLauncher<Intent> signInLauncher, SignInCallback signInCallback) {
        this.context = context;
        this.signInLauncher = signInLauncher;
        this.signInCallback = signInCallback;

        initializeGoogleSignInClient();
    }

    private void initializeGoogleSignInClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new com.google.android.gms.common.api.Scope(GmailScopes.GMAIL_READONLY))
                .requestServerAuthCode(context.getString(R.string.web_client_id), true)
                .requestIdToken(context.getString(R.string.web_client_id))
                .build();
        googleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    public void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    public void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                setupGmailService(account);
                signInCallback.onSignInSuccess(account);
            }
        } catch (ApiException e) {
            signInCallback.onSignInFailure("Sign-in failed: " + e.getMessage());
        }
    }

    private void setupGmailService(GoogleSignInAccount account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(GmailScopes.GMAIL_READONLY));
        credential.setSelectedAccount(account.getAccount());
        gmailService = new Gmail.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Your App Name")
                .build();
    }

    public List<String> fetchAllEmailIDs() {
        List<String> emailIDs = new ArrayList<>();
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                ListMessagesResponse messagesResponse = gmailService.users().messages().list("me").execute();
                List<Message> messageList = messagesResponse.getMessages();
                Log.d("GmailServices", "Fetched " + messageList.size() + " emails.");

                if (messageList != null) {
                    for (Message message : messageList) {
                        emailIDs.add(message.getId());
                    }
                }

                signInCallback.onEmailIDsFetched(emailIDs);

            } catch (GoogleJsonResponseException e) {
                Log.e("GmailServices", "Google API error: " + e.getMessage(), e);
                showLog("Google API error: " + e.getMessage());
                signInCallback.onEmailIDsFetched(null);
            } catch (IOException e) {
                Log.e("GmailServices", "Network error: " + e.getMessage(), e);
                showLog("Network error: " + e.getMessage());
                signInCallback.onEmailIDsFetched(null);
            } catch (Exception e) {
                Log.e("GmailServices", "Unknown error occurred", e);
                showLog("Unknown error occurred");
                signInCallback.onEmailIDsFetched(null);
            }
        });
        return emailIDs;
    }

    public interface FetchEmailCallback {
        void onEmailFetched(Message message);
        void onFetchEmailFailed(String errorMessage);
    }

    public void fetchEmailById(String id, FetchEmailCallback callback) {
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                Message message = gmailService.users().messages().get("me", id).execute();
                callback.onEmailFetched(message);
            } catch (IOException e) {
                Log.e("GmailServices", "Error fetching email: " + e.getMessage());
                callback.onFetchEmailFailed("Error fetching email: " + e.getMessage());
            }
        });
    }

    private void showLog(String message) {
        Log.d("GmailServices", message);
    }

    public interface SignInCallback {
        void onSignInSuccess(GoogleSignInAccount account);

        void onSignInFailure(String errorMessage);

        void onEmailIDsFetched(List<String> emailIDs);
    }
}