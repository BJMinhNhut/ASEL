package com.cs426.asel.ui.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.cs426.asel.R;
import com.cs426.asel.databinding.FragmentEventsBinding;
import com.google.android.material.tabs.TabLayout;

public class EventsFragment extends Fragment {
    private FragmentEventsBinding binding;
    private Fragment calendarFragment, listFragment;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        EventsViewModel eventsViewModel =
                new ViewModelProvider(this).get(EventsViewModel.class);

        binding = FragmentEventsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        TabLayout tabLayout = binding.dashboardTab;

        calendarFragment = new EventsCalendarFragment();
        listFragment = new EventsListFragment();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                switch (tab.getPosition()) {
                    case 0:
                        ft.show(calendarFragment).hide(listFragment);
                        break;
                    case 1:
                        ft.show(listFragment).hide(calendarFragment);
                        break;
                }
                ft.commit();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.fragmentContainer, calendarFragment);
        ft.add(R.id.fragmentContainer, listFragment);
        ft.show(calendarFragment).hide(listFragment);
        ft.commit();

//        final TextView textView = binding.textDashboard;
//        dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}