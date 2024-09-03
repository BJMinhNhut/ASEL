package com.cs426.asel.ui.account;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.cs426.asel.ui.account.AccountViewModel;

public class AccountViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;

    public AccountViewModelFactory(Context context) {
        this.context = context.getApplicationContext(); // Use application context to avoid leaks
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AccountViewModel.class)) {
            return (T) new AccountViewModel(context);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
