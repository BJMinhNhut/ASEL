package com.cs426.asel.backend;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
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
    private ExecutorService executorService;

    public GmailServices(Context context, GoogleSignInAccount account) {
        this.context = context;
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(GmailScopes.GMAIL_READONLY));
        credential.setSelectedAccount(account.getAccount());
        Toast.makeText(context, "Account name: " + account.getDisplayName(), Toast.LENGTH_SHORT).show();
        gmailService = new Gmail.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Your App Name")
                .build();
    }

    public void fetchEmails(EmailFetchListener listener) {
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                Log.d("GmailServices", "Fetching emails...");

                // Perform the network request to fetch the list of messages
                ListMessagesResponse messagesResponse = gmailService.users().messages().list("me").execute();
                List<Message> messageList = messagesResponse.getMessages();

                List<Message> fullMessages = new ArrayList<>();
                if (messageList != null) {
                    Log.d("GmailServices", "Number of messages: " + messageList.size());
                    // Limit the number of messages processed to a maximum of 5
                    int messagesToFetch = Math.min(5, messageList.size());
                    for (int i = 0; i < messagesToFetch; i++) {
                        Message message = messageList.get(i);
                        // Fetch each message's full details using its ID
                        Message fullMessage = gmailService.users().messages().get("me", message.getId()).execute();
                        fullMessages.add(fullMessage);
                    }
                    Log.d("GmailServices", "Fetched " + fullMessages.size() + " messages");
                    for (Message message : fullMessages) {
                        Log.d("GmailServices", "Message ID: " + message.getId());
                        Log.d("GmailServices", "Message Snippet: " + message.getSnippet());
                    }
                    Log.d("GmailServices", "Emails fetched successfully");
                }

                // Pass the full messages to the listener
                if (listener != null) {
                    listener.onEmailsFetched(fullMessages);
                }

            } catch (GoogleJsonResponseException e) {
                // Handle Google API-specific exceptions
                Log.e("GmailServices", "Google API error: " + e.getMessage(), e);
                showToast("Google API error: " + e.getMessage());
                if (listener != null) {
                    listener.onEmailsFetched(null);
                }
            } catch (IOException e) {
                // Handle general network and I/O exceptions
                Log.e("GmailServices", "Network error: " + e.getMessage(), e);
                showToast("Network error: " + e.getMessage());
                if (listener != null) {
                    listener.onEmailsFetched(null);
                }
            } catch (Exception e) {
                // Handle other exceptions
                Log.e("GmailServices", "Unknown error occurred", e);
                showToast("Unknown error occurred");
                if (listener != null) {
                    listener.onEmailsFetched(null);
                }
            } finally {
                // Shutdown the executor service to free up resources
                executorService.shutdown();
            }
        });
    }

    private void showToast(String message) {
        // Show toast on the main thread
        if (context != null) {
            ((android.app.Activity) context).runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
        }
    }

    public interface EmailFetchListener {
        void onEmailsFetched(List<Message> messages);
    }
}
