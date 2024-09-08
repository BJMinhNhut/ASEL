package com.cs426.asel;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.cs426.asel.ui.account.AccountViewModel;
import com.cs426.asel.ui.account.AccountViewModelFactory;
import com.cs426.asel.ui.emails.EmailsViewModel;
import com.cs426.asel.ui.emails.EmailsViewModelFactory;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

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

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_emails)
                .build();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);

        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Set up listener for tab selection instead of using OnDestinationChangedListener
        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                // Clear the back stack and navigate to Home
                clearBackStackAndNavigate(navController, R.id.navigation_home);
                return true; // Home tab is selected
            } else {
                // Clear back stack, navigate to Home first, then to the selected tab
                clearBackStackAndNavigate(navController, R.id.navigation_home);
                navController.navigate(itemId); // Navigate to the selected tab
                return true; // Another tab is selected
            }
        });

        // Initialize ViewModels (AccountViewModel, EmailsViewModel)
        initializeViewModels();
    }

    private void clearBackStackAndNavigate(NavController navController, int destinationId) {
        // Clear the back stack, but keep the Home fragment
        navController.popBackStack(R.id.navigation_home, false); // False to leave Home fragment in the stack
        navController.navigate(destinationId); // Navigate to the target fragment (e.g., Home or another tab)
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

}
