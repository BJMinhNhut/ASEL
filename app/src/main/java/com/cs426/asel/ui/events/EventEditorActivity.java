package com.cs426.asel.ui.events;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cs426.asel.backend.Event;
import com.cs426.asel.backend.EventRepository;
import com.cs426.asel.backend.Mail;
import com.cs426.asel.backend.MailRepository;
import com.cs426.asel.backend.Utility;
import com.cs426.asel.databinding.ActivityEventEditorBinding;
import com.google.android.material.textfield.TextInputEditText;

import java.time.Instant;

public class EventEditorActivity extends AppCompatActivity {

    private ActivityEventEditorBinding binding;
    private int eventType = 0;
    private Event curEvent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEventEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Bundle extras = getIntent().getExtras();
        String userEmail = extras.getString("userEmail");

        TextInputEditText dateEditText = binding.fromDate;
        TextInputEditText timeEditText = binding.fromTime;
        TextInputEditText toDateEditText = binding.toDate;
        TextInputEditText toTimeEditText = binding.toTime;
        dateEditText.setOnClickListener(v -> chooseDate(dateEditText));
        timeEditText.setOnClickListener(v -> chooseTime(timeEditText));
        toDateEditText.setOnClickListener(v -> chooseDate(toDateEditText));
        toTimeEditText.setOnClickListener(v -> chooseTime(toTimeEditText));

        binding.remind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReminderOptions();
            }

            private void showReminderOptions() {
                String[] options = new String[]{"5 minutes before", "15 minutes before", "30 minutes before", "1 hour before", "1 day before"};
                String selected = binding.remind.getText().toString();

                AlertDialog.Builder builder = new AlertDialog.Builder(EventEditorActivity.this);
                builder.setTitle("Remind me:");
                builder.setSingleChoiceItems(options, 0, (dialog, which) -> {
                    binding.remind.setText(options[which]);
                });

                builder.setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                });

                builder.show();
            }
        });

        binding.eventType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.taskRadioButton.getId()) {
                switchToTaskLayout();
            } else {
                switchToEventLayout();
            }
        });

        binding.allDaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                binding.fromTimeBoxLayout.setVisibility(View.VISIBLE);
                if (eventType == 0) {
                    binding.toTimeBoxLayout.setVisibility(View.VISIBLE);
                }
            } else {
                binding.fromTimeBoxLayout.setVisibility(View.GONE);
                binding.toTimeBoxLayout.setVisibility(View.GONE);
            }
        });

        binding.saveEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventRepository eventRepository = new EventRepository(EventEditorActivity.this, userEmail);

                putEventInfo();
                // If event not int db, insert then publish, else find the event and publish
                if (curEvent.getID() == -1) {
                    curEvent.setIsPublished(true);
                    eventRepository.insertEvent(curEvent);
                } else {
                    eventRepository.setPublishEvent(curEvent.getID(), true);
                }

                finish();
            }
        });

        if (extras == null || !extras.containsKey("emailId") || extras.getString("emailId") == null) {
            curEvent = new Event();
        } else {
            Mail mail = new MailRepository(this, userEmail).getMailById(extras.getString("emailId"));
            if (mail == null) {
                return;
            }
            curEvent = mail.getEvent();
            autofillEvent();
        }
    }

    private void putEventInfo() {
        curEvent.setTitle(binding.title.getText().toString());
        curEvent.setDescription(binding.description.getText().toString());
        curEvent.setLocation(binding.location.getText().toString());
    }

    private void autofillEvent() {
        Bundle extras = getIntent().getExtras();
        if (extras == null || !extras.containsKey("emailId") || extras.getString("emailId") == null) {
            return;
        }

        String emailId = extras.getString("emailId");
        String userEmail = extras.getString("userEmail");
        Mail mail = new MailRepository(this, userEmail).getMailById(emailId);
        if (mail == null) {
            return;
        }

        Instant fromDateTime = mail.getEvent().getStartTime();
        String fromDateString = Utility.parseInstant(fromDateTime, "dd/MM/yyyy");
        String fromTimeString = Utility.parseInstant(fromDateTime, "HH:mm");

        binding.fromDate.setText(fromDateString);
        binding.fromTime.setText(fromTimeString);

        int duration = mail.getEvent().getDuration();
        if (duration > 0) {
            switchToEventLayout();
            Instant toDateTime = fromDateTime.plusSeconds(60 * duration);
            String toDateString = Utility.parseInstant(toDateTime, "dd/MM/yyyy");
            String toTimeString = Utility.parseInstant(toDateTime, "HH:mm");

            binding.toDate.setText(toDateString);
            binding.toTime.setText(toTimeString);
        } else {
            binding.toDate.setText(fromDateString);
            binding.toTime.setText(fromTimeString);
            switchToTaskLayout();
        }

        binding.location.setText(mail.getEvent().getLocation());
        binding.title.setText(mail.getTitle());
        binding.description.setText(mail.getSummary());
    }

    private void chooseDate(TextInputEditText dateEditText) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this);
        datePickerDialog.setOnDateSetListener((view, year, month, dayOfMonth) -> {
            dateEditText.setText(String.format("%d/%d/%d", month, dayOfMonth, year));

            // TODO: add validation for date time
        });
        datePickerDialog.show();
    }

    private void chooseTime(TextInputEditText timeEditText) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            timeEditText.setText(String.format("%02d:%02d", hourOfDay, minute));
        }, 0, 0, false);
        timePickerDialog.show();
    }

    private void switchToTaskLayout() {
        eventType = 1;
        binding.toDateLayout.setVisibility(View.GONE);
    }

    private void switchToEventLayout() {
        eventType = 0;
        binding.toDateLayout.setVisibility(View.VISIBLE);
        if (binding.allDaySwitch.isChecked()) {
            binding.toTimeBoxLayout.setVisibility(View.GONE);
        } else {
            binding.toTimeBoxLayout.setVisibility(View.VISIBLE);
        }
    }
}
