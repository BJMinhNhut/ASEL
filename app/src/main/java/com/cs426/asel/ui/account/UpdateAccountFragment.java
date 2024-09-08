package com.cs426.asel.ui.account;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cs426.asel.R;
import com.cs426.asel.backend.Mail;
import com.cs426.asel.ui.emails.EmailsViewModel;

import java.util.List;

public class UpdateAccountFragment extends Fragment {

    private AccountViewModel accountViewModel;
    private EmailsViewModel emailsViewModel;
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

        // Obtain ViewModels from the activity's ViewModelProvider
        accountViewModel = new ViewModelProvider(requireActivity()).get(AccountViewModel.class);
        emailsViewModel = new ViewModelProvider(requireActivity()).get(EmailsViewModel.class);

        TextView accountName = view.findViewById(R.id.text_account_name);

        // Observe signInResult LiveData
        accountViewModel.getSignInResult().observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                accountName.setText(account.getDisplayName());  // Update the UI with the signed-in account's name
            } else {
                accountName.setText("No account selected");     // Update the UI when signed out
            }
        });

        Button addAccount = view.findViewById(R.id.button_add_account);
        addAccount.setOnClickListener(v -> addAccount());

        Button logOut = view.findViewById(R.id.log_out);
        logOut.setOnClickListener(v -> logOut());
    }

    private void logOut() {
        accountViewModel.signOut();
    }

    private void testMail() {
        Mail mail = new Mail(emailsViewModel.getMessages().get(0));
//        mail.summarize();

        System.out.println(mail.getId());
        System.out.println(mail.getTitle());
        System.out.println(mail.getSender());
        System.out.println(mail.getSummary());
        System.out.println(mail.getContent());

    }

    private void printEmailContent() {

    }

    private void fetchEmailContent() {

    }

    private void fetchEmailIds() {
        emailsViewModel.fetchAllEmailsID(); // Use EmailsViewModel
    }

    private void addAccount() {
        accountViewModel.signIn();
    }
}
