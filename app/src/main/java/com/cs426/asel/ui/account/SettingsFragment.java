package com.cs426.asel.ui.account;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import com.cs426.asel.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment {

    private RadioGroup themeGroup, emailNotificationGroup, reminderNotificationGroup;
    private SharedPreferences sharedPreferences;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        themeGroup = view.findViewById(R.id.theme_group);
        emailNotificationGroup = view.findViewById(R.id.email_notification_group);
        reminderNotificationGroup = view.findViewById(R.id.reminder_notification_group);

        // Load saved preferences
        loadPreferences();

        // Set listeners for RadioGroups
        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.light_theme) {
                savePreference("theme", "light");
            } else if (checkedId == R.id.dark_theme) {
                savePreference("theme", "dark");
            } else if (checkedId == R.id.system_theme) {
                savePreference("theme", "system");
            }
        });

        emailNotificationGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.email_notification_on) {
                savePreference("new_email_notification", true);
            } else {
                savePreference("new_email_notification", false);
            }
        });

        reminderNotificationGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.reminder_notification_on) {
                savePreference("reminder_notification", true);
            } else {
                savePreference("reminder_notification", false);
            }
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getParentFragmentManager();
                fm.popBackStack();
            }
        });

        return view;
    }

    private void savePreference(String key, Object value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        }
        editor.apply();
    }

    private void loadPreferences() {
        // Load and set the saved theme preference
        String savedTheme = sharedPreferences.getString("theme", "light");
        switch (savedTheme) {
            case "light":
                themeGroup.check(R.id.light_theme);
                break;
            case "dark":
                themeGroup.check(R.id.dark_theme);
                break;
            case "system":
                themeGroup.check(R.id.system_theme);
                break;
        }

        // Load and set the new email notification preference
        boolean newEmailNotification = sharedPreferences.getBoolean("new_email_notification", true);
        if (newEmailNotification) {
            emailNotificationGroup.check(R.id.email_notification_on);
        } else {
            emailNotificationGroup.check(R.id.email_notification_off);
        }

        // Load and set the reminder notification preference
        boolean reminderNotification = sharedPreferences.getBoolean("reminder_notification", true);
        if (reminderNotification) {
            reminderNotificationGroup.check(R.id.reminder_notification_on);
        } else {
            reminderNotificationGroup.check(R.id.reminder_notification_off);
        }
    }
}