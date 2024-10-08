package com.cs426.asel.ui.account;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import static android.app.Activity.RESULT_OK;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.cs426.asel.MainActivity;
import com.cs426.asel.R;
import com.cs426.asel.backend.ChatGPTUtils;
import com.cs426.asel.backend.Utility;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class UpdateInfoFragment extends Fragment implements MainActivity.PermissionCallback{

    private TextRecognizer textRecognizer;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private TextInputEditText editTextFullName, editTextStudentId, editTextSchool, editTextFaculty, editTextDegree;
    private TextInputEditText textViewBirthday;
    private ImageButton imageButtonAvatar;
    private ImageView buttonBack, buttonCamera;
    private MaterialButton buttonSave;
    private Calendar calendar;
    private InfoViewModel mViewModel;
    private boolean isCameraPermitted = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_update_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(InfoViewModel.class);
        // Initialize UI components
        imageButtonAvatar = view.findViewById(R.id.imageButtonAvatar);
        buttonCamera = view.findViewById(R.id.buttonCamera);
        editTextFullName = view.findViewById(R.id.editTextFullName);
        editTextStudentId = view.findViewById(R.id.editTextStudentId);
        textViewBirthday = view.findViewById(R.id.textViewBirthday);
        editTextSchool = view.findViewById(R.id.editTextSchool);
        editTextFaculty = view.findViewById(R.id.editTextFaculty);
        editTextDegree = view.findViewById(R.id.editTextDegree);
        buttonBack = view.findViewById(R.id.buttonBack);
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

        // Initialize Text Recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Setup camera launcher to capture an image
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

        // Set listener for camera button to open the camera
        buttonCamera.setOnClickListener(v -> {
            openCamera();
            Toast.makeText(requireContext(), "Camera opened", Toast.LENGTH_SHORT).show();
        });

        // Load saved data
        loadStudentInfo();
        saveViewModel();

        // Set onClick listener for back button
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
        buttonCamera.setOnClickListener(v -> {
            openCamera();
        });

        // Set onClick listener for save button
        buttonSave.setOnClickListener(v -> {
            saveStudentInfo();
            saveViewModel();
            Toast.makeText(requireContext(), "Info saved successfully", Toast.LENGTH_SHORT).show();
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

//        observeInfo();
    }

    private void observeInfo() {
        mViewModel.getFullName().observe(getViewLifecycleOwner(), fullName -> editTextFullName.setText(fullName));
        mViewModel.getStudentId().observe(getViewLifecycleOwner(), studentId -> editTextStudentId.setText(studentId));
        mViewModel.getBirthdate().observe(getViewLifecycleOwner(), birthdate -> textViewBirthday.setText(birthdate));
        mViewModel.getSchool().observe(getViewLifecycleOwner(), school -> editTextSchool.setText(school));
        mViewModel.getFaculty().observe(getViewLifecycleOwner(), faculty -> editTextFaculty.setText(faculty));
        mViewModel.getDegree().observe(getViewLifecycleOwner(), degree -> editTextDegree.setText(degree));

        // Observe avatar and set the image in ImageView
        mViewModel.getAvatar().observe(getViewLifecycleOwner(), avatarBase64 -> {
            if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                byte[] decodedBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
                Bitmap avatarBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                imageButtonAvatar.setImageBitmap(avatarBitmap);
            } else {
                imageButtonAvatar.setImageResource(R.drawable.avatar_default); // Set default avatar if none
            }
        });
    }

    private String encodeToBase64(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] byteArray = baos.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
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
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        Intent cameraIntent = new Intent(MediaStore.ACTION_PICK_IMAGES);
        cameraLauncher.launch(cameraIntent);
    }

//    private void runTextRecognition(Bitmap bitmap) {
//        InputImage image = InputImage.fromBitmap(bitmap, 0);
//        textRecognizer.process(image)
//                .addOnSuccessListener(texts -> {
//                    // Handle successful OCR
//                    Log.d("OCR", texts.getText());
////                    extractUserInfo(texts.getText());
//                })
//                .addOnFailureListener(e -> {
//                    Log.e("OCR", "Text recognition failed", e);
//                });
//    }

//    private Uri saveBitmapToFile(Bitmap bitmap) {
//        File tempFile = new File(getContext().getCacheDir(), "temp_image.jpg");
//        try {
//            FileOutputStream outputStream = new FileOutputStream(tempFile);
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
//            outputStream.flush();
//            outputStream.close();
//            return FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", tempFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    private void extractUserInfo(Bitmap bitmap) {
        String prompt = "Extract user information of the following text using the schema: { \"fullName\": str, \"studentId\": str, \"birthday\": str, \"degree\": str, \"school\": str, \"faculty\": str}. Birthday should be in the format \"dd/MM/yyyy\". Unclear info should be null: ";
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

    private void setStudentInfo(String text) {
        try {
            StudentInfo studentInfo = new ObjectMapper().readValue(text, StudentInfo.class);
            if (studentInfo.fullName == null || studentInfo.studentId == null || studentInfo.birthday == null || studentInfo.school == null || studentInfo.faculty == null) {
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(requireContext(), "Failed to extract user info from image", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            editTextFullName.setText(studentInfo.fullName);
            editTextStudentId.setText(studentInfo.studentId);
            textViewBirthday.setText(studentInfo.birthday);
            editTextDegree.setText(studentInfo.degree);
            editTextSchool.setText(studentInfo.school);
            editTextFaculty.setText(studentInfo.faculty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void saveImageToPreferences(Bitmap bitmap) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("StudentInfo-" + getUserEmail(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("avatar_image", encodeToBase64(bitmap));
        editor.apply();
        // Update ViewModel with the new avatar image
        mViewModel.setAvatar(encodeToBase64(bitmap));
    }

    private String getUserEmail() {
        return Utility.getUserEmail(requireContext());
    }

    private Bitmap loadImageFromPreferences() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("StudentInfo-" + getUserEmail(), Context.MODE_PRIVATE);
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
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("StudentInfo-" + getUserEmail(), Context.MODE_PRIVATE);
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
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("StudentInfo-" + getUserEmail(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("full_name", editTextFullName.getText().toString());
        editor.putString("student_id", editTextStudentId.getText().toString());
        editor.putString("birthday", textViewBirthday.getText().toString());
        editor.putString("school", editTextSchool.getText().toString());
        editor.putString("faculty", editTextFaculty.getText().toString());
        editor.putString("degree", editTextDegree.getText().toString());
        editor.apply();
    }

    private void saveViewModel() {
        mViewModel.setFullName(editTextFullName.getText().toString());
        mViewModel.setStudentId(editTextStudentId.getText().toString());
        mViewModel.setBirthdate(textViewBirthday.getText().toString());
        mViewModel.setSchool(editTextSchool.getText().toString());
        mViewModel.setFaculty(editTextFaculty.getText().toString());
        mViewModel.setDegree(editTextDegree.getText().toString());

        // Set the avatar image to the ViewModel
        Bitmap avatarBitmap = ((BitmapDrawable) imageButtonAvatar.getDrawable()).getBitmap();
        mViewModel.setAvatar(encodeToBase64(avatarBitmap));
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


