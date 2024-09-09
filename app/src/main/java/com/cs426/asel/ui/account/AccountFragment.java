package com.cs426.asel.ui.account;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.cs426.asel.R;
import com.cs426.asel.databinding.FragmentAccountBinding;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private TextView accountNameView;
    private ImageView accountAvatarView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AccountViewModel accountViewModel =
                new ViewModelProvider(requireActivity()).get(AccountViewModel.class);

        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize views
        accountNameView = binding.accountNameView;
        accountAvatarView = binding.accountAvatarView;

        // Load saved data
        loadAccountInfo();

        final LinearLayout accountButton = binding.updateAccountButton;
        accountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateAccount();
            }
        });

        final LinearLayout infoButton = binding.updateInfoButton;
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateInfo();
            }
        });

        final LinearLayout settingsButton = binding.settingsButton;
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });

        return root;
    }

    private void loadAccountInfo() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("StudentInfo", Context.MODE_PRIVATE);

            // Load the saved full name
            String fullName = sharedPreferences.getString("full_name", "Account Name");
            accountNameView.setText(fullName);

            mainHandler.post(() -> {
                // Load the saved avatar image
                Bitmap avatar = loadImageFromPreferences(sharedPreferences);
                if (avatar != null) {
                    accountAvatarView.setImageBitmap(avatar);
                } else {
                    accountAvatarView.setImageResource(R.drawable.profile_image_default); // Set default avatar
                }
            });
        }).start();
    }

    private Bitmap loadImageFromPreferences(SharedPreferences sharedPreferences) {
        String imageEncoded = sharedPreferences.getString("avatar_image", null);
        if (imageEncoded != null) {
            byte[] decodedBytes = Base64.decode(imageEncoded, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        }
        return null;
    }

    private void updateAccount() {
        FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.accountContainer, new UpdateAccountFragment()).addToBackStack(null).commit();
    }

    private void updateInfo() {
        FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.accountContainer, new UpdateInfoFragment()).addToBackStack(null).commit();
    }

    private void openSettings() {
        FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.accountContainer, new SettingsFragment()).addToBackStack(null).commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
