package com.cs426.asel.backend;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public final class Utility {
    public static String getUserEmail(Context context) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            return null;
        }
        return account.getEmail();
    }
}
