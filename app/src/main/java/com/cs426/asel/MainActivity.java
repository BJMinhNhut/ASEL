package com.cs426.asel;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.cs426.asel.backend.GmailServices;
import com.cs426.asel.ui.account.UpdateAccountFragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.cs426.asel.databinding.ActivityMainBinding;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message; // Import statement added here

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements UpdateAccountFragment.OnFragmentInteractionListener
{

    private ActivityMainBinding binding;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_emails)
                .build();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);

        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.navView, navController);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(GmailScopes.GMAIL_READONLY))
                .requestServerAuthCode(getString(R.string.web_client_id), true) // This forces consent dialog
                .requestIdToken(getString(R.string.web_client_id))
                .build();
        Log.d("MainActivity", "Starting sign-in process...");
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleSignInResult(task);
                    }
                });

    }

    @Override
    public void addAccount() {
        // Sign out of the current session
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // After signing out, revoke access to ensure a fresh permission request
            mGoogleSignInClient.revokeAccess().addOnCompleteListener(this, revokeTask -> {
                // Now initiate the sign-in flow again
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                signInLauncher.launch(signInIntent);
            });
        });
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, show authenticated UI.
            GmailServices gmailServices = new GmailServices(this, account);
            gmailServices.fetchEmails(new GmailServices.EmailFetchListener() {
                @Override
                public void onEmailsFetched(List<Message> messages) {
                    Log.d("MainActivity", "Start onEmailsFetched");
                    // Process and display the messages
                    if (messages != null && !messages.isEmpty()) {
                        Log.d("MainActivity", "Number of messages kakaka: " + messages.size());
                        // Get the first message
                        Message firstMessage = messages.get(0);
                        Log.d("MainActivity", "First Message ID: " + firstMessage.getId());

                        // Extract the snippet from the message (the snippet is a brief part of the message)
                        String snippet = firstMessage.getSnippet();

                        Log.d("MainActivity", "First Snippet: " + snippet);
                    } else {
                        Log.d("MainActivity", "No messages found");
                    }
                }
            });
        } catch (ApiException e) {
            // Sign in failed, update UI appropriately
            Toast.makeText(this, "Sign-in failed", Toast.LENGTH_SHORT).show();
        }
    }
}
