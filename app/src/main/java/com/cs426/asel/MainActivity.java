package com.cs426.asel;

import android.Manifest;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.cs426.asel.backend.Utility;
import com.cs426.asel.ui.account.AccountContainer;
import com.cs426.asel.ui.account.AccountViewModel;
import com.cs426.asel.ui.account.AccountViewModelFactory;
import com.cs426.asel.ui.account.InfoViewModel;
import com.cs426.asel.ui.emails.EmailsContainer;
import com.cs426.asel.ui.events.EventsContainer;
import com.cs426.asel.ui.events.EventsFragment;
import com.cs426.asel.ui.emails.EmailsViewModel;
import com.cs426.asel.ui.emails.EmailsViewModelFactory;
import com.cs426.asel.ui.home.HomeFragment;
import com.cs426.asel.ui.notifications.NotificationsFragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.cs426.asel.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private AccountViewModel accountViewModel;
    private EmailsViewModel emailsViewModel; // Initialize here
    private ActivityResultLauncher<Intent> signInLauncher;
    private InfoViewModel infoViewModel;

    private static final int REQUEST_CODE_POST_NOTIFICATION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = binding.navView;
        ViewPager2 viewPager = binding.viewPager;

        viewPager.setOffscreenPageLimit(4);
        viewPager.setAdapter(new ScreenSlidePagerAdapter(this));
        navView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_home) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (id == R.id.navigation_events) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (id == R.id.navigation_emails) {
                viewPager.setCurrentItem(2);
                return true;
            } else if (id == R.id.navigation_account) {
                viewPager.setCurrentItem(3);
                return true;
            }

            return false;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        navView.setSelectedItemId(R.id.navigation_home);
                        break;
                    case 1:
                        navView.setSelectedItemId(R.id.navigation_events);
                        break;
                    case 2:
                        navView.setSelectedItemId(R.id.navigation_emails);
                        break;
                    case 3:
                        navView.setSelectedItemId(R.id.navigation_account);
                        break;
                }
            }
        });

        // Initialize ViewModels (AccountViewModel, EmailsViewModel)
        initializeViewModels();
        loadStudentInfoToViewModel();
        loadTheme();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_CODE_POST_NOTIFICATION);
        }
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (!alarmManager.canScheduleExactAlarms()) {
            // Direct the user to the system settings to allow exact alarms\
            new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app requires permission to schedule exact alarms to notify you about important events.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    // Open the system settings for exact alarms
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
            String channelId = "event_channel_id";
            CharSequence name = "Event Reminder";
            String description = "Notifications for upcoming events";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);

            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        Utility.startScheduledWork(this);
    }

    private static final int HOME_FRAGMENT_POSITION = 0; // Position of HomeFragment in ViewPager2

    public void refreshHomeFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        String homeFragmentTag = "f" + HOME_FRAGMENT_POSITION; // Use the tag format to find the fragment

        HomeFragment homeFragment = (HomeFragment) fragmentManager.findFragmentByTag(homeFragmentTag);

        if (homeFragment != null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.detach(homeFragment);
            fragmentTransaction.attach(homeFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        } else {
            Toast.makeText(this, "HomeFragment not found", Toast.LENGTH_SHORT).show();
        }
    }

    private static class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new HomeFragment();
                case 1:
                    return new EventsContainer();
                case 2:
                    return new EmailsContainer();
                case 3:
                    return new AccountContainer();
                default:
                    return new HomeFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }

    private void initializeViewModels() {
        // Initialize AccountViewModel
        AccountViewModelFactory accountFactory = new AccountViewModelFactory(getApplicationContext());
        accountViewModel = new ViewModelProvider(this, accountFactory).get(AccountViewModel.class);

        // Initialize EmailsViewModel using the factory
        EmailsViewModelFactory emailsFactory = new EmailsViewModelFactory(getApplicationContext());
        emailsViewModel = new ViewModelProvider(this, emailsFactory).get(EmailsViewModel.class);

        // Register ActivityResultLauncher for sign-in and pass it to the ViewModel
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        accountViewModel.onSignInSuccess(task.getResult());
                    } else {
                        accountViewModel.onSignInFailure("Sign-in canceled or failed");
                        Toast.makeText(this, "Sign-in canceled or failed", Toast.LENGTH_SHORT).show();
                    }
                });

        // Pass the launcher to the AccountViewModel
        accountViewModel.setSignInLauncher(signInLauncher);

        infoViewModel = new ViewModelProvider(this).get(InfoViewModel.class);
    }

    private void loadStudentInfoToViewModel() {
        SharedPreferences sharedPreferences = getSharedPreferences("StudentInfo", Context.MODE_PRIVATE);
        infoViewModel.setFullName(sharedPreferences.getString("full_name", ""));
        infoViewModel.setStudentId(sharedPreferences.getString("student_id", ""));
        infoViewModel.setBirthdate(sharedPreferences.getString("birthday", ""));
        infoViewModel.setSchool(sharedPreferences.getString("school", ""));
        infoViewModel.setFaculty(sharedPreferences.getString("faculty", ""));
        infoViewModel.setDegree(sharedPreferences.getString("degree", ""));
        infoViewModel.setAvatar(sharedPreferences.getString("avatar_image", "")); // Set avatar
    }

    private void loadTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String savedTheme = sharedPreferences.getString("theme", "light");
        if (savedTheme.equals("light")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (savedTheme.equals("dark")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public interface PermissionCallback {
        void onPermissionResult(boolean isGranted);
    }
}
