package com.cs426.asel.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.cs426.asel.R;

public class AccountContainer extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.container_account, container, false);
        // why? https://stackoverflow.com/questions/7508044/android-fragment-no-view-found-for-id
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.add(R.id.accountContainer, new AccountFragment()).commit();

        return view;
    }
}
