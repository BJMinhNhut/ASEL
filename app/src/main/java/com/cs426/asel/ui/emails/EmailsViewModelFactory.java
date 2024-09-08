package com.cs426.asel.ui.emails;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.List;

public class EmailsViewModelFactory implements ViewModelProvider.Factory {

    private final Context context;

    public EmailsViewModelFactory(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(EmailsViewModel.class)) {
            return (T) new EmailsViewModel(context);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
