package com.cs426.asel.ui.emails;

import android.content.Intent;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.cs426.asel.R;
import com.cs426.asel.backend.Mail;
import com.cs426.asel.backend.MailRepository;
import com.cs426.asel.backend.Utility;
import com.cs426.asel.databinding.FragmentEmailDetailBinding;
import com.cs426.asel.ui.account.AccountViewModel;
import com.cs426.asel.ui.events.EventEditorActivity;

public class EmailDetailFragment extends Fragment {
    private static String emailId;
    private Mail mail;
    private FragmentEmailDetailBinding binding;

    public static EmailDetailFragment newInstance(Mail mail) {
        Bundle args = new Bundle();
        args.putString("emailId", emailId);

        EmailDetailFragment fragment = new EmailDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_email_detail, container, false);
        AccountViewModel accountViewModel = new ViewModelProvider(requireActivity()).get(AccountViewModel.class);

        binding = FragmentEmailDetailBinding.bind(view);
        emailId = getArguments().getString("emailId");
        mail = new MailRepository(requireContext(), Utility.getUserEmail(requireContext())).getMailById(emailId);

        binding.emailTitle.setText(mail.getTitle());
        binding.emailSender.setText(mail.getSender());
        binding.emailBody.setText(mail.getContent());
        binding.emailDatetime.setText(mail.getSentTime());

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button createEventButton = view.findViewById(R.id.create_event_button);
        createEventButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EventEditorActivity.class);
            intent.putExtra("emailId", emailId);
            startActivity(intent);
        });

        Button moveToReadButton = view.findViewById(R.id.move_to_read_button);
        moveToReadButton.setOnClickListener(v -> {
            // TODO: Move email to read
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getParentFragmentManager();
                fm.popBackStack();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
