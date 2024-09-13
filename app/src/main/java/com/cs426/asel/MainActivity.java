package com.cs426.asel;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.cs426.asel.ui.account.AccountContainer;
import com.cs426.asel.ui.account.AccountViewModel;
import com.cs426.asel.ui.account.AccountViewModelFactory;
import com.cs426.asel.ui.emails.EmailsContainer;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.cs426.asel.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private AccountViewModel accountViewModel;
    private EmailsViewModel emailsViewModel; // Initialize here
    private ActivityResultLauncher<Intent> signInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = binding.navView;
        ViewPager2 viewPager = binding.viewPager;

        viewPager.setOffscreenPageLimit(5);
        viewPager.setAdapter(new ScreenSlidePagerAdapter(this));
        navView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_home) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (id == R.id.navigation_events) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (id == R.id.navigation_notifications) {
                viewPager.setCurrentItem(2);
                return true;
            } else if (id == R.id.navigation_account) {
                viewPager.setCurrentItem(3);
                return true;
            } else if (id == R.id.navigation_emails) {
                viewPager.setCurrentItem(4);
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
                        navView.setSelectedItemId(R.id.navigation_notifications);
                        break;
                    case 3:
                        navView.setSelectedItemId(R.id.navigation_account);
                        break;
                    case 4:
                        navView.setSelectedItemId(R.id.navigation_emails);
                        break;
                }
            }
        });

        // Initialize ViewModels (AccountViewModel, EmailsViewModel)
        initializeViewModels();
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
                    return new EventsFragment();
                case 2:
                    return new NotificationsFragment();
                case 3:
                    return new AccountContainer();
                case 4:
                    return new EmailsContainer();
                default:
                    return new HomeFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
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
    }

    public interface PermissionCallback {
        void onPermissionResult(boolean isGranted);
    }
}
