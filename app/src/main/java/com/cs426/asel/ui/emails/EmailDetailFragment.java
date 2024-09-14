package com.cs426.asel.ui.emails;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.cs426.asel.R;
import com.cs426.asel.backend.Mail;
import com.cs426.asel.backend.MailRepository;
import com.cs426.asel.backend.Utility;
import com.cs426.asel.databinding.FragmentEmailDetailBinding;
import com.cs426.asel.ui.account.AccountViewModel;
import com.cs426.asel.ui.events.EventEditorFragment;

public class EmailDetailFragment extends Fragment {
    private static String emailId;
    private Mail mail;
    private FragmentEmailDetailBinding binding;
    private MailRepository mailRepository;

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
        mailRepository = new MailRepository(requireContext(), Utility.getUserEmail(requireContext()));
        emailId = getArguments().getString("emailId");
        mail = mailRepository.getMailById(emailId);

        String sender = mail.getSender();
        String senderName = sender.substring(0, sender.indexOf("@"));
        String senderDomain = sender.substring(sender.indexOf("@"));

        binding.subject.setText(mail.getTitle());
        binding.senderName.setText(senderName);
        binding.senderDomain.setText(senderDomain);
        binding.body.setText(mail.getContent());
        binding.sendTime.setText(mail.getSentTime());

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView backButton = view.findViewById(R.id.back);

        Button createEventButton = view.findViewById(R.id.create_event);
        createEventButton.setOnClickListener(v -> {
            EventEditorFragment eventEditorFragment = new EventEditorFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("eventId", mail.getEvent().getID());
            eventEditorFragment.setArguments(bundle);

            FragmentTransaction ft = getParentFragmentManager().beginTransaction();
            ft.replace(R.id.emailsContainer, eventEditorFragment).addToBackStack(null).commit();
        });

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
