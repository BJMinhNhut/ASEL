package com.cs426.asel.ui.account;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cs426.asel.R;
import com.cs426.asel.backend.GoogleAccountServices;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.Gson;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

public class AccountViewModel extends ViewModel implements GoogleAccountServices.SignInCallback {
    private final MutableLiveData<GoogleSignInAccount> signInResult = new MutableLiveData<>();
    private GoogleAccountServices googleAccountServices;
    private Context context;

    public AccountViewModel(Context context) {
        this.context = context;
        Log.d("AccountViewModel", "AccountViewModel created!");
    }

    public LiveData<GoogleSignInAccount> getSignInResult() {
        return signInResult;
    }

    public void setSignInLauncher(ActivityResultLauncher<Intent> launcher) {
        googleAccountServices = new GoogleAccountServices(context, launcher, this);
        silentSignIn();
    }

    public void signIn() {
        if (googleAccountServices != null) {
            googleAccountServices.signIn();
        } else {
            Log.e("AccountViewModel", "GoogleAccountServices is null!");
        }
    }

    public void signOut() {
        if (googleAccountServices != null) {
            googleAccountServices.signOut();
        } else {
            Log.e("AccountViewModel", "GoogleAccountServices is null!");
            changeAccount(null);
        }
    }

    public void silentSignIn() {
        Log.d("AccountViewModel", "silentSignIn() called!");
        if (googleAccountServices != null) {
            googleAccountServices.silentSignIn();
        } else {
            Log.e("AccountViewModel", "GoogleAccountServices is null!");
            changeAccount(null);
        }
    }

    @Override
    public void onSignInSuccess(GoogleSignInAccount account) {
        changeAccount(account);
    }

    @Override
    public void onSignInFailure(String errorMessage) {
        Log.e("AccountViewModel", "Sign-in failed: " + errorMessage);
    }

    @Override
    public void onSignOutSuccess() {
        changeAccount(null);
    }

    @Override
    public void onSignOutFailure(String errorMessage) {
        Log.e("AccountViewModel", "Sign-out failed: " + errorMessage);
    }

    public void changeAccount(GoogleSignInAccount account) {
        signInResult.setValue(account);
    }

    public String getUserEmail() {
        GoogleSignInAccount account = signInResult.getValue();
        if (account != null) {
            return account.getEmail();
        } else {
            return null;
        }
    }
}
