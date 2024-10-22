package com.cs426.asel.ui.account;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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

        TextView accountEmail = view.findViewById(R.id.text_account_email);
        TextView accountName = view.findViewById(R.id.text_account_name);

        // Observe signInResult LiveData
        accountViewModel.getSignInResult().observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                accountEmail.setText(account.getEmail());
                accountName.setVisibility(View.VISIBLE);
                accountName.setText(account.getDisplayName());
            } else {
                accountEmail.setText("No account selected");
                accountName.setVisibility(View.GONE);
            }
        });

        Button addAccount = view.findViewById(R.id.button_add_account);
        addAccount.setOnClickListener(v -> addAccount());

        Button logOut = view.findViewById(R.id.log_out);
        logOut.setOnClickListener(v -> logOut());

        ImageView backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            FragmentManager fm = getParentFragmentManager();
            fm.popBackStack();
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getParentFragmentManager();
                fm.popBackStack();
            }
        });
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
        emailsViewModel.fetchNextIdBatch(); // Use EmailsViewModel
    }

    private void addAccount() {
        accountViewModel.signIn();
    }
}
