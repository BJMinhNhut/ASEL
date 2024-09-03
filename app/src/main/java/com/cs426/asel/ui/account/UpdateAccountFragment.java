package com.cs426.asel.ui.account;

import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cs426.asel.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.api.services.gmail.model.Message;

import java.util.List;

public class UpdateAccountFragment extends Fragment {

    private AccountViewModel mViewModel;
    private List<String> emailIDs;

    public static UpdateAccountFragment newInstance() {
        return new UpdateAccountFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_update_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(AccountViewModel.class);

        TextView accountName = view.findViewById(R.id.text_account_name);
        accountName.setText(mViewModel.getCurrentSignedInAccount().getDisplayName());

        Button addAccount = view.findViewById(R.id.button_add_account);
        addAccount.setOnClickListener(v -> addAccount());

        Button fetchEmailIds = view.findViewById(R.id.fetch_email_ids);
        fetchEmailIds.setOnClickListener(v -> fetchEmailIds());

        Button fetchEmailContent = view.findViewById(R.id.fetch_email_content);
        fetchEmailContent.setOnClickListener(v -> fetchEmailContent());

        Button printEmailContent = view.findViewById(R.id.print_email_content);
        printEmailContent.setOnClickListener(v -> printEmailContent());
    }

    private void printEmailContent() {
        emailIDs = mViewModel.getEmailsID();
        for (int i = 0; i < Math.min(5, emailIDs.size()); i++) {
            Message message = mViewModel.getMessage(emailIDs.get(i));
            Log.d("Email ID", "ID " + emailIDs.get(i) + ": " + message.getSnippet());
        }
    }

    private void fetchEmailContent() {
        emailIDs = mViewModel.getEmailsID();
        for (int i = 0; i < Math.min(5, emailIDs.size()); i++) {
            Log.d("Email ID", "Fetching ID " + i + ": " + emailIDs.get(i));
            mViewModel.fetchEmailContent(emailIDs.get(i));
        }
    }

    private void fetchEmailIds() {
        mViewModel.fetchAllEmailsID();
    }

    private void addAccount() {
        // Sign out of the current session
        GoogleSignInAccount account = mViewModel.getCurrentSignedInAccount();
        if (account != null) {
            GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().addOnCompleteListener(requireActivity(), task -> {
                // After signing out, revoke access to ensure a fresh permission request
                GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).revokeAccess().addOnCompleteListener(requireActivity(), revokeTask -> {
                    // Now initiate the sign-in flow again
                    mViewModel.signIn();
                });
            });
        }
    }
}
