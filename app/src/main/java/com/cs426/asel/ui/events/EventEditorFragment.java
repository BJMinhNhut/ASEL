package com.cs426.asel.ui.events;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.cs426.asel.backend.Event;
import com.cs426.asel.backend.EventRepository;
import com.cs426.asel.backend.Mail;
import com.cs426.asel.backend.MailRepository;
import com.cs426.asel.backend.Notification;
import com.cs426.asel.backend.Utility;
import com.cs426.asel.databinding.FragmentEventEditorBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.journeyapps.barcodescanner.Util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

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

        binding.repeatModeText.setText("Does not repeat");
        binding.remindBeforeText.setText("5 minutes");

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

        binding.saveEventButton.setOnClickListener(v -> {
            EventRepository eventRepository = new EventRepository(requireContext(), Utility.getUserEmail(requireContext()));

            putEventInfo();
            long eventID = -1;
            // If event not int db, insert then publish, else find the event and publish
            if (curEvent.getID() == -1) {
                curEvent.setIsPublished(true);
                eventID = eventRepository.insertEvent(curEvent);
            } else {
                eventRepository.updateEvent(curEvent);
                eventID = curEvent.getID();
                eventRepository.setPublishEvent(eventID, true);
                Utility.cancelNotification(requireContext(), (int)eventID);
            }
            int repeatMode = curEvent.isRepeating() ? Notification.stringToRepeatMode(curEvent.getRepeatFrequency()) : Notification.REPEAT_NONE;
            Calendar startTimeCalendar = Utility.toCalendar(curEvent.getStartTime());
            Calendar reminderTimeCalendar = Utility.toCalendar(curEvent.getReminderTime());
            long time_diff = (startTimeCalendar.getTimeInMillis() - reminderTimeCalendar.getTimeInMillis()) / (1000 * 60);
            String title = time_diff + " minutes before " + curEvent.getTitle();
            Notification noti = new Notification(title, curEvent.getDescription(), Utility.toCalendar(curEvent.getStartTime()), repeatMode, Utility.toCalendar(curEvent.getReminderTime()));

            Utility.scheduleNotification(requireContext(), (int)eventID, noti);

            FragmentManager fm = getParentFragmentManager();
            fm.popBackStack();
        });

        Bundle extras = getArguments();
        if (extras == null || !extras.containsKey("emailId") || extras.getString("emailId") == null) {
            curEvent = new Event();
        } else {
            Mail mail = new MailRepository(requireContext(), Utility.getUserEmail(requireContext())).getMailById(extras.getString("emailId"));
            curEvent = mail.getEvent();
            autofillEvent();
        }

        if (curEvent.isPublished()) {
            binding.deleteEventButton.setVisibility(View.VISIBLE);
            binding.deleteEventButton.setOnClickListener(v -> {
                EventRepository eventRepository = new EventRepository(requireContext(), Utility.getUserEmail(requireContext()));
                assert curEvent.getID() != -1 : "Event not in db yet to delete";
                int eventID = curEvent.getID();
                eventRepository.setPublishEvent(eventID, false);
                Utility.cancelNotification(requireContext(), eventID);
                FragmentManager fm = getParentFragmentManager();
                fm.popBackStack();
            });
        } else {
            binding.deleteEventButton.setVisibility(View.GONE);
        }

        return root;
    }

    private void putEventInfo() {
        if (!checkInputValid()) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        curEvent.setTitle(binding.title.getText().toString());
        curEvent.setDescription(binding.descriptionText.getText().toString());
        String startTime = "00:00";
        if (!binding.allDaySwitch.isChecked()) {
            startTime = binding.startTimeText.getText().toString();
        }
        String startDateTime = binding.startDateText.getText().toString() + " " + startTime;
        Instant startDateTimeInstant = Utility.parseToInstant(startDateTime, "d/MM/yyyy HH:mm");
        curEvent.setStartTime(startDateTimeInstant);

        if (binding.eventTypeTab.getSelectedTabPosition() == 0) {
            String endTime = "00:00";
            if (!binding.allDaySwitch.isChecked()) {
                endTime = binding.endTimeText.getText().toString();
            }
            String endDateTime = binding.endDateText.getText().toString() + " " + endTime;
            Instant endTimeInstant = Utility.parseToInstant(endDateTime, "d/MM/yyyy HH:mm");
            curEvent.setDuration((int) ChronoUnit.MINUTES.between(startDateTimeInstant, endTimeInstant));
        } else {
            curEvent.setDuration(0);
        }

        String remindBefore = binding.remindBeforeText.getText().toString();
        switch (remindBefore) {
            case "5 minutes":
                curEvent.setReminderTime(startDateTimeInstant.minusSeconds(5 * 60));
                break;
            case "15 minutes":
                curEvent.setReminderTime(startDateTimeInstant.minusSeconds(15 * 60));
                break;
            case "30 minutes":
                curEvent.setReminderTime(startDateTimeInstant.minusSeconds(30 * 60));
                break;
            case "1 hour":
                curEvent.setReminderTime(startDateTimeInstant.minusSeconds(60 * 60));
                break;
            case "1 day":
                curEvent.setReminderTime(startDateTimeInstant.minusSeconds(24 * 60 * 60));
                break;
            default:
                curEvent.setReminderTime(startDateTimeInstant.minusSeconds(5 * 60));
                break;
        }

        if (binding.locationText.getText() != null && !binding.locationText.getText().toString().isEmpty())
            curEvent.setLocation(binding.locationText.getText().toString());
    }

    private boolean checkInputValid() {
        if (binding.title.getText() == null || binding.title.getText().toString().isEmpty()) {
            return  false;
        }

        if (binding.startDateText.getText() == null || binding.startDateText.getText().toString().isEmpty()) {
            return false;
        }

        if (!binding.allDaySwitch.isChecked()) {
            if (binding.startTimeText.getText() == null
                    || binding.startTimeText.getText().toString().isEmpty()) {
                return false;
            }

        }

        if (binding.eventTypeTab.getSelectedTabPosition() == 0) {
            if (binding.endDateText.getText() == null || binding.endDateText.getText().toString().isEmpty())
                return false; // Event must have end date
            if (!binding.allDaySwitch.isChecked() && (binding.endTimeText.getText() == null
                    || binding.endTimeText.getText().toString().isEmpty())) {
                return false;
            }
        }

        if (binding.repeatModeText.getText() == null || binding.repeatModeText.getText().toString().isEmpty())
            return false;

        if (binding.remindBeforeText.getText() == null || binding.remindBeforeText.getText().toString().isEmpty())
            return false;

        return true;
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
            dateEditText.setText(String.format("%02d/%02d/%02d", dayOfMonth, month + 1, year));

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
        binding.startDateLayout.setHint("On date");
        binding.startTimeLayout.setHint("At time");
        binding.endDateLayout.setVisibility(View.GONE);
        binding.endTimeLayout.setVisibility(View.GONE);
    }

    private void switchToEventLayout() {
        eventType = 0;
        binding.startDateLayout.setHint("Start date");
        binding.startTimeLayout.setHint("Start time");
        binding.endDateLayout.setVisibility(View.VISIBLE);
        if (binding.allDaySwitch.isChecked()) {
            binding.endTimeLayout.setVisibility(View.GONE);
        } else {
            binding.endTimeLayout.setVisibility(View.VISIBLE);
        }
    }
}
