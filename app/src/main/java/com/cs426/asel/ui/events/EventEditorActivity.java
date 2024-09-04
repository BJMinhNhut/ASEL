package com.cs426.asel.ui.events;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cs426.asel.databinding.ActivityEventEditorBinding;
import com.google.android.material.textfield.TextInputEditText;

import org.w3c.dom.Text;

public class EventEditorActivity extends AppCompatActivity {

    private ActivityEventEditorBinding binding;
    private int eventType = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEventEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

        binding.saveEventButton.setOnClickListener(v -> {
            // TODO: Save event
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
