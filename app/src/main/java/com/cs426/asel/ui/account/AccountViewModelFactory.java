package com.cs426.asel.ui.account;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class AccountViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;

    // Constructor accepts both Context and onAccountChanged listener
    public AccountViewModelFactory(Context context) {
        this.context = context.getApplicationContext(); // Use application context to avoid leaks
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AccountViewModel.class)) {
            // Pass both context and accountChangedListener to the ViewModel
            return (T) new AccountViewModel(context);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
