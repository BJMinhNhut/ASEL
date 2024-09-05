package com.cs426.asel.ui.account;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.util.Log;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.api.services.gmail.model.Message;

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
        emailsViewModel = new ViewModelProvider(requireActivity()).get(EmailsViewModel.class); // No need for a factory here

        TextView accountName = view.findViewById(R.id.text_account_name);
        GoogleSignInAccount account = accountViewModel.getCurrentSignedInAccount();
        if (account != null) {
            accountName.setText(account.getDisplayName());
        }

        Button addAccount = view.findViewById(R.id.button_add_account);
        addAccount.setOnClickListener(v -> addAccount());

        Button fetchEmailIds = view.findViewById(R.id.fetch_email_ids);
        fetchEmailIds.setOnClickListener(v -> fetchEmailIds());

        Button fetchEmailContent = view.findViewById(R.id.fetch_email_content);
        fetchEmailContent.setOnClickListener(v -> fetchEmailContent());

        Button printEmailContent = view.findViewById(R.id.print_email_content);
        printEmailContent.setOnClickListener(v -> printEmailContent());

        Button testMail = view.findViewById(R.id.test_mail);
        testMail.setOnClickListener(v -> testMail());
    }

    private void testMail() {
        Mail mail = new Mail(emailsViewModel.getMessages().get(0));
//        mail.summarize();

        System.out.println(mail.getEmailID());
        System.out.println(mail.getTitle());
        System.out.println(mail.getSender());
        System.out.println(mail.getSummary());
        System.out.println(mail.getContent());

    }

    private void printEmailContent() {
        emailIDs = emailsViewModel.getEmailsID(); // Use EmailsViewModel for email-related operations
        for (int i = 0; i < Math.min(5, emailIDs.size()); i++) {
            Message message = emailsViewModel.getMessage(emailIDs.get(i)); // Use EmailsViewModel
            if (message != null) {
                Log.d("Email ID", "ID " + emailIDs.get(i) + ": " + message.getSnippet());
            }
        }
    }

    private void fetchEmailContent() {
        emailIDs = emailsViewModel.getEmailsID(); // Use EmailsViewModel
//        for (int i = 0; i < Math.min(5, emailIDs.size()); i++) {
//            Log.d("Email ID", "Fetching ID " + i + ": " + emailIDs.get(i));
//            emailsViewModel.fetchEmailContent(emailIDs.get(i)); // Use EmailsViewModel
//        }
    }

    private void fetchEmailIds() {
        emailsViewModel.fetchAllEmailsID(); // Use EmailsViewModel
    }

    private void addAccount() {
        // Sign out of the current session
        GoogleSignInAccount account = accountViewModel.getCurrentSignedInAccount();
        if (account != null) {
            GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .signOut()
                    .addOnCompleteListener(requireActivity(), task -> {
                        // After signing out, revoke access to ensure a fresh permission request
                        GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .revokeAccess()
                                .addOnCompleteListener(requireActivity(), revokeTask -> {
                                    // Now initiate the sign-in flow again
                                    accountViewModel.signIn();
                                });
                    });
        }
    }
}
