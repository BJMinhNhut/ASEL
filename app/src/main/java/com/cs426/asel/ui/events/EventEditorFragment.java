package com.cs426.asel.ui.events;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.cs426.asel.backend.Event;
import com.cs426.asel.backend.EventRepository;
import com.cs426.asel.backend.Mail;
import com.cs426.asel.backend.MailRepository;
import com.cs426.asel.backend.Utility;
import com.cs426.asel.databinding.FragmentEventEditorBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.time.Instant;

public class EventEditorFragment extends Fragment {

    private FragmentEventEditorBinding binding;
    private int eventType = 0;
    private Event curEvent;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEventEditorBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        TextInputEditText dateEditText = binding.startDateText;
        TextInputEditText timeEditText = binding.startTimeText;
        TextInputEditText toDateEditText = binding.endDateText;
        TextInputEditText toTimeEditText = binding.endTimeText;
        dateEditText.setOnClickListener(v -> chooseDate(dateEditText));
        timeEditText.setOnClickListener(v -> chooseTime(timeEditText));
        toDateEditText.setOnClickListener(v -> chooseDate(toDateEditText));
        toTimeEditText.setOnClickListener(v -> chooseTime(toTimeEditText));

        binding.repeatModeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRepeatOptions();
            }

            private void showRepeatOptions() {
                String[] options = new String[]{"Does not repeat", "Daily", "Weekly", "Monthly", "Annually"};
                String selected = binding.repeatModeText.getText().toString();
                int selectedIndex = 0; // Default to the first option

                // Find the index of the current selected option if it matches any in the options
                for (int i = 0; i < options.length; i++) {
                    if (options[i].equals(selected)) {
                        selectedIndex = i;
                        break;
                    }
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Repeat mode:");

                // Use an array to hold the selected index inside the listener
                final int[] tempSelectedIndex = {selectedIndex};

                builder.setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    tempSelectedIndex[0] = which;
                });

                builder.setPositiveButton("OK", (dialog, which) -> {
                    // Set the text to the option that was selected
                    binding.repeatModeText.setText(options[tempSelectedIndex[0]]);
                    dialog.dismiss();
                });

                builder.show();
            }
        });


        binding.remindBeforeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReminderOptions();
            }

            private void showReminderOptions() {
                String[] options = new String[]{"5 minutes", "15 minutes", "30 minutes", "1 hour", "1 day"};
                String selected = binding.remindBeforeText.getText().toString();
                int selectedIndex = 0; // Default to the first option

                // Find the index of the current selected option if it matches any in the options
                for (int i = 0; i < options.length; i++) {
                    if (options[i].equals(selected)) {
                        selectedIndex = i;
                        break;
                    }
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Remind me before:");

                // Use an array to hold the selected index inside the listener
                final int[] tempSelectedIndex = {selectedIndex};

                builder.setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    tempSelectedIndex[0] = which;
                });

                builder.setPositiveButton("OK", (dialog, which) -> {
                    // Set the text to the option that was selected
                    binding.remindBeforeText.setText(options[tempSelectedIndex[0]]);
                    dialog.dismiss();
                });

                builder.show();
            }
        });

        binding.eventTypeTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        switchToEventLayout();
                        break;
                    case 1:
                        switchToTaskLayout();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.allDaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                binding.startTimeLayout.setVisibility(View.VISIBLE);
                if (eventType == 0) {
                    binding.endTimeLayout.setVisibility(View.VISIBLE);
                }
            } else {
                binding.startTimeLayout.setVisibility(View.GONE);
                binding.endTimeLayout.setVisibility(View.GONE);
            }
        });

        binding.backButton.setOnClickListener(v -> {
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

        binding.saveEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventRepository eventRepository = new EventRepository(requireContext(), Utility.getUserEmail(requireContext()));

                putEventInfo();
                // If event not int db, insert then publish, else find the event and publish
                if (curEvent.getID() == -1) {
                    curEvent.setIsPublished(true);
                    eventRepository.insertEvent(curEvent);
                } else {
                    eventRepository.updateEvent(curEvent);
                }
            }
        });

        Bundle extras = getArguments();

        if (extras == null || !extras.containsKey("emailId") || extras.getString("emailId") == null) {
            curEvent = new Event();
        } else {
            Mail mail = new MailRepository(requireContext(), Utility.getUserEmail(requireContext())).getMailById(extras.getString("emailId"));
            curEvent = mail.getEvent();
            autofillEvent();
        }

        return root;
    }

    private void putEventInfo() {
        curEvent.setTitle(binding.title.getText().toString());
        curEvent.setDescription(binding.descriptionText.getText().toString());
        curEvent.setLocation(binding.locationText.getText().toString());
    }

    private void autofillEvent() {
        Bundle extras = getArguments();
        if (extras == null || !extras.containsKey("emailId") || extras.getString("emailId") == null) {
            return;
        }

        String emailId = extras.getString("emailId");
        String userEmail = extras.getString("userEmail");
        Mail mail = new MailRepository(requireContext(), Utility.getUserEmail(requireContext())).getMailById(emailId);
        if (mail == null) {
            return;
        }

        Instant fromDateTime = mail.getEvent().getStartTime();
        String fromDateString = Utility.parseInstant(fromDateTime, "dd/MM/yyyy");
        String fromTimeString = Utility.parseInstant(fromDateTime, "HH:mm");

        binding.startDateText.setText(fromDateString);
        binding.startTimeText.setText(fromTimeString);

        int duration = mail.getEvent().getDuration();
        if (duration > 0) {
            switchToEventLayout();
            Instant toDateTime = fromDateTime.plusSeconds(60 * duration);
            String toDateString = Utility.parseInstant(toDateTime, "dd/MM/yyyy");
            String toTimeString = Utility.parseInstant(toDateTime, "HH:mm");

            binding.endDateText.setText(toDateString);
            binding.endTimeText.setText(toTimeString);
        } else {
            binding.endDateText.setText(fromDateString);
            binding.endTimeText.setText(fromTimeString);
            switchToTaskLayout();
        }

        binding.locationText.setText(mail.getEvent().getLocation());
        binding.title.setText(mail.getTitle());
        binding.descriptionText.setText(mail.getSummary());
    }

    private void chooseDate(TextInputEditText dateEditText) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext());
        datePickerDialog.setOnDateSetListener((view, year, month, dayOfMonth) -> {
            Log.d("EventEditorFragment", "year: " + year + ", month: " + (month + 1) + ", dayOfMonth: " + dayOfMonth);
            dateEditText.setText(String.format("%d/%d/%d", month + 1, dayOfMonth, year));

            // TODO: add validation for date time
        });
        datePickerDialog.show();
    }

    private void chooseTime(TextInputEditText timeEditText) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            timeEditText.setText(String.format("%02d:%02d", hourOfDay, minute));
        }, 0, 0, false);
        timePickerDialog.show();
    }

    private void switchToTaskLayout() {
        eventType = 1;
        binding.endDateLayout.setVisibility(View.GONE);
        binding.endTimeLayout.setVisibility(View.GONE);
    }

    private void switchToEventLayout() {
        eventType = 0;
        binding.endDateLayout.setVisibility(View.VISIBLE);
        if (binding.allDaySwitch.isChecked()) {
            binding.endTimeLayout.setVisibility(View.GONE);
        } else {
            binding.endTimeLayout.setVisibility(View.VISIBLE);
        }
    }
}
