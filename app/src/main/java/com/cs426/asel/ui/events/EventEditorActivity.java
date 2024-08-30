package com.cs426.asel.ui.events;

import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cs426.asel.databinding.ActivityEventEditorBinding;

public class EventEditorActivity extends AppCompatActivity {

    private ActivityEventEditorBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

        binding = ActivityEventEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


    }
}
