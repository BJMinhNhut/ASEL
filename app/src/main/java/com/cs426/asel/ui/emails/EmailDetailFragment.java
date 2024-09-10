package com.cs426.asel.ui.emails;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.cs426.asel.R;
import com.cs426.asel.backend.Mail;
import com.cs426.asel.ui.events.EventEditorActivity;

public class EmailDetailFragment extends Fragment {
    private static int emailId;

    public static EmailDetailFragment newInstance(Mail mail) {
        Bundle args = new Bundle();
        args.putInt("emailId", emailId);

        EmailDetailFragment fragment = new EmailDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_email_detail, container, false);

        emailId = getArguments().getInt("emailId");

        // TODO: Set email details in the view

        // Bind onClick

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
