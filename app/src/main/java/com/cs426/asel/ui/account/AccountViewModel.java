package com.cs426.asel.ui.account;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cs426.asel.backend.GmailServices;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.services.gmail.model.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountViewModel extends ViewModel implements GmailServices.SignInCallback {
    private final MutableLiveData<GoogleSignInAccount> signInResult = new MutableLiveData<>();
    private GmailServices gmailServices;
    private Map<String, Message> messageCache;
    private Context context;

    public AccountViewModel(Context context) {
        this.context = context;
        Log.d("AccountViewModel", "AccountViewModel created!");

        messageCache = new HashMap<>();
    }

    public LiveData<GoogleSignInAccount> getSignInResult() {
        return signInResult;
    }

    public GoogleSignInAccount getCurrentSignedInAccount() {
        return GoogleSignIn.getLastSignedInAccount(context);
    }

    public void setSignInLauncher(ActivityResultLauncher<Intent> launcher) {
        gmailServices = new GmailServices(context, launcher, this);
    }

    public void signIn() {
        if (gmailServices != null) {
            gmailServices.signIn();
        } else {
            Log.e("AccountViewModel", "gmailServices is null!");
        }
    }

    @Override
    public void onSignInSuccess(GoogleSignInAccount account) {
        signInResult.setValue(account);
    }

    @Override
    public void onSignInFailure(String errorMessage) {
        signInResult.setValue(null);
    }

    @Override
    public void onEmailIDsFetched(List<String> emailIDs) {
        if (emailIDs != null) {
            reset();
            for (String id : emailIDs) {
                storeID(id);
            }
            Log.d("AccountViewModel", "All email IDs fetched and stored.");
        }
    }

    public void fetchAllEmailsID() {
        if (gmailServices != null) {
            reset();
            gmailServices.fetchAllEmailIDs();
        }
    }

    public void fetchEmailContent(String id) {
        if (messageCache.containsKey(id) && messageCache.get(id) != null) {
            // Return cached message if already fetched
            return;
        } else if (gmailServices != null) {
            gmailServices.fetchEmailById(id, new GmailServices.FetchEmailCallback() {
                @Override
                public void onEmailFetched(Message message) {
                    storeMessage(id, message);
                    Log.d("AccountViewModel", "Email ID " + id + " fetched and stored.");
                    // Update LiveData or notify UI here if needed
                }

                @Override
                public void onFetchEmailFailed(String errorMessage) {
                    Log.e("AccountViewModel", errorMessage);
                    // Handle error case
                }
            });
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

    public GmailServices getGmailServices() {
        return gmailServices;
    }
}
