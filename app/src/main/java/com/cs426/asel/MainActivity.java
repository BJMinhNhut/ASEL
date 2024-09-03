package com.cs426.asel;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.cs426.asel.ui.account.AccountViewModel;
import com.cs426.asel.ui.account.AccountViewModelFactory;
import com.cs426.asel.ui.account.UpdateAccountFragment;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.cs426.asel.databinding.ActivityMainBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AccountViewModel accountViewModel;
    private ActivityResultLauncher<Intent> signInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);

        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Initialize AccountViewModel
        AccountViewModelFactory factory = new AccountViewModelFactory(getApplicationContext());
        accountViewModel = new ViewModelProvider(this, factory).get(AccountViewModel.class);

        // Register ActivityResultLauncher for sign-in and pass it to the ViewModel
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

                        // Call GmailServices to handle the sign-in result through the ViewModel
                        accountViewModel.getGmailServices().handleSignInResult(task);
                    } else {
                        accountViewModel.getGmailServices().handleSignInResult(null);
                        Toast.makeText(this, "Sign-in canceled or failed", Toast.LENGTH_SHORT).show();
                    }
                });

        // Pass the launcher to the ViewModel
        accountViewModel.setSignInLauncher(signInLauncher);

        // Observe the sign-in result from ViewModel
        accountViewModel.getSignInResult().observe(this, account -> {
            if (account != null) {
                // Handle successful sign-in, e.g., update UI or proceed with fetching emails
                Toast.makeText(MainActivity.this, "Welcome, " + account.getDisplayName(), Toast.LENGTH_SHORT).show();
            } else {
                // Handle sign-in failure
                Toast.makeText(MainActivity.this, "Sign-in failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
