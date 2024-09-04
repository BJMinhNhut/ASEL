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

public class AccountViewModel extends ViewModel implements GmailServices.SignInCallback {
    private final MutableLiveData<GoogleSignInAccount> signInResult = new MutableLiveData<>();
    private GmailServices gmailServices;
    private Context context;

    public AccountViewModel(Context context) {
        this.context = context;
        Log.d("AccountViewModel", "AccountViewModel created!");
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
}
