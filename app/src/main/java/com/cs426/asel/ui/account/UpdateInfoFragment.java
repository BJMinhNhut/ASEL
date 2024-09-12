package com.cs426.asel.ui.account;

import static com.cs426.asel.backend.ChatGPTUtils.getResponse;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cs426.asel.MainActivity;
import com.cs426.asel.R;
import com.cs426.asel.backend.ChatGPTUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UpdateInfoFragment extends Fragment implements MainActivity.PermissionCallback {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private AccountViewModel mViewModel;
    private TextInputEditText editTextFullName, editTextStudentId, editTextSchool, editTextFaculty, editTextDegree;
    private TextInputEditText textViewBirthday;
    private ImageButton imageButtonAvatar;
    private MaterialButton buttonSave, cameraButton;
    private Calendar calendar;
    private boolean isCameraPermitted = false;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_update_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(AccountViewModel.class);

        // Initialize UI components
        imageButtonAvatar = view.findViewById(R.id.imageButtonAvatar);
        cameraButton = view.findViewById(R.id.camera_button);
        editTextFullName = view.findViewById(R.id.editTextFullName);
        editTextStudentId = view.findViewById(R.id.editTextStudentId);
        textViewBirthday = view.findViewById(R.id.textViewBirthday);
        editTextSchool = view.findViewById(R.id.editTextSchool);
        editTextFaculty = view.findViewById(R.id.editTextFaculty);
        editTextDegree = view.findViewById(R.id.editTextDegree);
        buttonSave = view.findViewById(R.id.buttonSave);

        // Initialize Calendar for date picker
        calendar = Calendar.getInstance();

        // Set listener for birthday TextView to show DatePickerDialog
        textViewBirthday.setOnClickListener(v -> showDatePickerDialog());

        // Set listener for ImageButton to open image picker
        imageButtonAvatar.setOnClickListener(v -> openImagePicker());

        // Initialize the image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri imageUri = data.getData();
                            setImageFromUri(imageUri);
                        }
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Bundle bundle = result.getData().getExtras();
                        Bitmap bitmap = (Bitmap) bundle.get("data");
                        extractUserInfo(bitmap);
                    }
                }
        );

        // Load saved data
        loadStudentInfo();

        ImageView buttonBack = view.findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> {
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

        // Set listener for camera button to open image picker
        checkCameraPermission();
        cameraButton.setOnClickListener(v -> {
            openCamera();
        });

        // Set onClick listener for save button
        buttonSave.setOnClickListener(v -> {
            saveStudentInfo();
            Toast.makeText(requireContext(), "Info saved successfully", Toast.LENGTH_SHORT).show();
            FragmentManager fm = getParentFragmentManager();
            fm.popBackStack();
        });
    }

    private void checkCameraPermission() {
        ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    onPermissionResult(isGranted);
                });

        if (ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            isCameraPermitted = true;
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void extractUserInfo(Bitmap bitmap) {
        String prompt = "Extract user information from the image following the schema: { \"fullName\": str, \"studentId\": str, \"birthday\": str, \"degree\": str, \"school\": str, \"faculty\": str}. Birthday should be in the format \"dd/MM/yyyy\". Unclear info should be null.";
        ListenableFuture<GenerateContentResponse> future = ChatGPTUtils.getResponse(prompt, bitmap);
        Executor executor = Executors.newSingleThreadExecutor();
        // TODO: start loading screen
        Futures.addCallback(
                future,
                new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        setStudentInfo(result.getText());
                        // TODO: stop loading screen
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e("UpdateInfoFragment", "Error extracting user info from image");
                    }
                },
                executor
        );
    }

    private void setStudentInfo(String text) {
        try {
            StudentInfo studentInfo = new ObjectMapper().readValue(text, StudentInfo.class);
            if (studentInfo.fullName == null || studentInfo.studentId == null || studentInfo.birthday == null || studentInfo.school == null || studentInfo.faculty == null) {
                Snackbar snackbar = Snackbar.make(requireView(), "Invalid student info", Snackbar.LENGTH_SHORT);
                snackbar.show();
                throw new Exception("Invalid student info");
            }

            editTextFullName.setText(studentInfo.fullName);
            editTextStudentId.setText(studentInfo.studentId);
            textViewBirthday.setText(studentInfo.birthday);
            editTextDegree.setText(studentInfo.degree);
            editTextSchool.setText(studentInfo.school);
            editTextFaculty.setText(studentInfo.faculty);
        } catch (Exception e) {
            e.printStackTrace();
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(requireContext(), "Failed to extract user info from image", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void setImageFromUri(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            imageButtonAvatar.setImageBitmap(bitmap);
            saveImageToPreferences(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageToPreferences(Bitmap bitmap) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("StudentInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String imageEncoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        editor.putString("avatar_image", imageEncoded);
        editor.apply();
    }

    private Bitmap loadImageFromPreferences() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("StudentInfo", Context.MODE_PRIVATE);
        String imageEncoded = sharedPreferences.getString("avatar_image", null);
        if (imageEncoded != null) {
            byte[] decodedBytes = Base64.decode(imageEncoded, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        }
        return null;
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    // Update calendar with selected date
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateBirthdayTextView();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void updateBirthdayTextView() {
        // Format the date to display in TextView
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        textViewBirthday.setText(dateFormat.format(calendar.getTime()));
    }

    private void loadStudentInfo() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("StudentInfo", Context.MODE_PRIVATE);
        editTextFullName.setText(sharedPreferences.getString("full_name", ""));
        editTextStudentId.setText(sharedPreferences.getString("student_id", ""));
        textViewBirthday.setText(sharedPreferences.getString("birthday", ""));
        editTextSchool.setText(sharedPreferences.getString("school", ""));
        editTextFaculty.setText(sharedPreferences.getString("faculty", ""));
        editTextDegree.setText(sharedPreferences.getString("degree", ""));

        // Load the saved avatar image if exists
        Bitmap avatar = loadImageFromPreferences();
        if (avatar != null) {
            imageButtonAvatar.setImageBitmap(avatar);
        } else {
            imageButtonAvatar.setImageResource(R.drawable.avatar_default); // Set default avatar
        }
    }

    private void saveStudentInfo() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("StudentInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("full_name", editTextFullName.getText().toString());
        editor.putString("student_id", editTextStudentId.getText().toString());
        editor.putString("birthday", textViewBirthday.getText().toString());
        editor.putString("school", editTextSchool.getText().toString());
        editor.putString("faculty", editTextFaculty.getText().toString());
        editor.putString("degree", editTextDegree.getText().toString());
        editor.apply();
    }

    @Override
    public void onPermissionResult(boolean isGranted) {
        isCameraPermitted = isGranted;
        if (!isGranted) {
            Snackbar snackbar = Snackbar.make(requireView(), "Camera permission denied", Snackbar.LENGTH_SHORT);
            snackbar.show();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StudentInfo {
        @JsonProperty("fullName")
        private String fullName;

        @JsonProperty("studentId")
        private String studentId;

        @JsonProperty("birthday")
        private String birthday;

        @JsonProperty("degree")
        private String degree;

        @JsonProperty("school")
        private String school;

        @JsonProperty("faculty")
        private String faculty;
    }
}

