package com.cs426.asel.backend;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

import java.io.IOException;
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
        gmailService = new Gmail.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Your App Name")
                .build();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void fetchEmails(EmailFetchListener listener) {
        executorService.submit(() -> {
            try {
                ListMessagesResponse messagesResponse = gmailService.users().messages().list("me").execute();
                List<Message> messages = messagesResponse.getMessages();
                if (listener != null) {
                    listener.onEmailsFetched(messages);
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (listener != null) {
                    listener.onEmailsFetched(null);
                }
            }
        });
    }

    public interface EmailFetchListener {
        void onEmailsFetched(List<Message> messages);
    }
}