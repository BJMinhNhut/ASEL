package com.cs426.asel.backend;

import static android.app.PendingIntent.getActivity;
import static java.security.AccessController.getContext;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;

import com.cs426.asel.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.gmail.GmailScopes;


public class GoogleAccountServices {
    private Context context;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;
    private SignInCallback signInCallback;

    public GoogleAccountServices(Context context, ActivityResultLauncher<Intent> signInLauncher, SignInCallback signInCallback) {
        this.context = context;
        this.signInLauncher = signInLauncher;
        this.signInCallback = signInCallback;
        initializeGoogleSignInClient();
    }

    private void initializeGoogleSignInClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(GmailScopes.GMAIL_READONLY))
                .requestIdToken(context.getString(R.string.web_client_id))
                .build();
        googleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    public void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    public void signOut() {
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                signInCallback.onSignOutSuccess();
            } else {
                signInCallback.onSignOutFailure("Sign-out failed");
            }
        });
    }

    public void silentSignIn() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account != null) {
            signInCallback.onSignInSuccess(account);
        } else {
            signIn();
        }
    }


    public void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                signInCallback.onSignInSuccess(account);
            }
        } catch (ApiException e) {
            signInCallback.onSignInFailure("Sign-in failed: " + e.getMessage());
        }
    }

    public interface SignInCallback {
        void onSignInSuccess(GoogleSignInAccount account);
        void onSignInFailure(String errorMessage);
        void onSignOutSuccess();
        void onSignOutFailure(String errorMessage);
    }
}
