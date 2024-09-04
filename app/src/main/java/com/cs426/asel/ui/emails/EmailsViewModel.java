package com.cs426.asel.ui.emails;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.cs426.asel.backend.GmailServices;
import com.google.api.services.gmail.model.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailsViewModel extends ViewModel implements GmailServices.EmailCallback {
    private GmailServices gmailServices;
    private Map<String, Message> messageCache;
    private Context context;

    public EmailsViewModel(Context context) {
        this.context = context;
        messageCache = new HashMap<>();
        gmailServices = new GmailServices(context, this); // Initialize GmailServices for email operations
    }

    public void fetchAllEmailsID() {
        gmailServices.fetchAllEmailIDs();
    }

    public void fetchEmailContent(String id) {
        if (messageCache.containsKey(id) && messageCache.get(id) != null) {
            return; // Return cached message if already fetched
        } else {
            gmailServices.fetchEmailById(id, new GmailServices.FetchEmailCallback() {
                @Override
                public void onEmailFetched(Message message) {
                    storeMessage(id, message);
                    Log.d("EmailsViewModel", "Email ID " + id + " fetched and stored.");
                }

                @Override
                public void onFetchEmailFailed(String errorMessage) {
                    Log.e("EmailsViewModel", errorMessage);
                }
            });
        }
    }

    @Override
    public void onEmailIDsFetched(List<String> emailIDs) {
        if (emailIDs != null) {
            reset();
            for (String id : emailIDs) {
                storeID(id);
            }
            Log.d("EmailsViewModel", "All email IDs fetched and stored.");
        }
    }

    public Message getMessage(String id) {
        return messageCache.getOrDefault(id, null);
    }

    public List<String> getEmailsID() {
        return new ArrayList<>(messageCache.keySet());
    }

    public void reset() {
        messageCache.clear();
    }

    public void storeID(String id) {
        if (!messageCache.containsKey(id)) {
            messageCache.put(id, null); // Placeholder for future message content
        }
    }

    public void storeMessage(String id, Message message) {
        if (message != null) {
            messageCache.put(id, message);
        }
    }
}
