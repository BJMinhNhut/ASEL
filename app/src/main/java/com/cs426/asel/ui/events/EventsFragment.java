package com.cs426.asel.ui.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs426.asel.R;
import com.cs426.asel.backend.Event;
import com.cs426.asel.backend.EventList;
import com.cs426.asel.backend.EventRepository;
import com.cs426.asel.backend.Utility;
import com.cs426.asel.databinding.FragmentEventsBinding;
import com.cs426.asel.ui.decoration.SpaceItemDecoration;
import com.cs426.asel.ui.emails.EmailDetailFragment;
import com.google.android.material.tabs.TabLayout;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class EventsFragment extends Fragment {
    private FragmentEventsBinding binding;
    private EventRepository eventRepository;
    private EventList ongoing, upcoming, completed;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        EventsViewModel eventsViewModel =
                new ViewModelProvider(this).get(EventsViewModel.class);

        binding = FragmentEventsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView eventRecyclerView = binding.eventRecyclerView;
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventRecyclerView.addItemDecoration(new SpaceItemDecoration(20));


        EventAdapter eventAdapter = new EventAdapter();
        eventRecyclerView.setAdapter(eventAdapter);

        binding.eventTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        eventAdapter.setEventList(ongoing);
                        break;
                    case 1:
                        eventAdapter.setEventList(upcoming);
                        break;
                    case 2:
                        eventAdapter.setEventList(completed);
                        break;
                    default:
                        eventAdapter.setEventList(ongoing);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.newEventFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                ft.replace(R.id.eventsContainer, new EventEditorFragment()).addToBackStack(null).commit();
            }
        });

        return root;
    }

    class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
        private EventList eventList;
        public EventAdapter(EventList eventList) {
            this.eventList = eventList;
        }

        public EventAdapter() {

        }

        public void setEventList(EventList newEventList) {
            this.eventList = newEventList;
            notifyDataSetChanged();
        }

        class EventViewHolder extends RecyclerView.ViewHolder {
            public TextView startDay, startMonth, toDate, endDay, endMonth;
            public TextView title, time, location, description;

            public EventViewHolder(View itemView) {
                super(itemView);
                startDay = itemView.findViewById(R.id.startDay);
                startMonth = itemView.findViewById(R.id.startMonth);
                toDate = itemView.findViewById(R.id.toDate);
                endDay = itemView.findViewById(R.id.endDay);
                endMonth = itemView.findViewById(R.id.endMonth);
                title = itemView.findViewById(R.id.title);
                time = itemView.findViewById(R.id.time);
                location = itemView.findViewById(R.id.location);
                description = itemView.findViewById(R.id.description);
            }
        }

        @NonNull
        @Override
        public EventAdapter.EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
            return new EventAdapter.EventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EventAdapter.EventViewHolder holder, int position) {
            Event event = eventList.getEvent(position);

            int duration = event.getDuration();

            LocalDateTime startDateTime = LocalDateTime.ofInstant(event.getStartTime(), ZoneId.systemDefault());

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM");
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd");

            String startTime = timeFormatter.format(startDateTime);
            String startDay = dayFormatter.format(startDateTime);
            String startMonth = monthFormatter.format(startDateTime);

            holder.startDay.setText(startDay);
            holder.startMonth.setText(startMonth);
            holder.toDate.setText("");
            holder.endDay.setText("");
            holder.endMonth.setText("");

            String time;
            if (event.isAllDay()) { // All-day
                LocalDateTime endDateTime = startDateTime.plusMinutes(event.getDuration());

                String endDay = dayFormatter.format(endDateTime);
                String endMonth = monthFormatter.format(endDateTime);

                if (startDateTime.getDayOfYear() == endDateTime.getDayOfYear() && startDateTime.getYear() == endDateTime.getYear()) { // Same day
                    time = startMonth + " " + startDay + ", all-day";
                } else {
                    time = startMonth + " " + startDay + " - " + endMonth + " " + endDay + ", all-day";

                    holder.toDate.setText("-");
                    holder.endDay.setText(endDay);
                    holder.endMonth.setText(endMonth);
                }
            } else if (duration > 0) { // Event
                LocalDateTime endDateTime = startDateTime.plusMinutes(event.getDuration());

                String endTime = timeFormatter.format(endDateTime);
                String endDay = dayFormatter.format(endDateTime);
                String endMonth = monthFormatter.format(endDateTime);

                if (startDateTime.getDayOfYear() == endDateTime.getDayOfYear() && startDateTime.getYear() == endDateTime.getYear()) { // Same day
                    time = startMonth + " " + startDay + ", " + startTime + " - " + endTime;
                } else {
                    time = startMonth + " " + startDay + ", " + startTime + " - " + endMonth + " " + endDay + ", " + endTime;

                    holder.toDate.setText("-");
                    holder.endDay.setText(endDay);
                    holder.endMonth.setText(endMonth);
                }
            } else { // Task
                time = startMonth + " " + startDay + ", " + startTime;
            }

            holder.title.setText(event.getTitle());
            holder.time.setText(time);
            holder.location.setText(event.getLocation());
            holder.description.setText(event.getDescription());

            holder.itemView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putInt("eventId", eventList.getEvent(position).getID()); // Replace 1 with the actual email ID you want to pass

                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                EventEditorFragment fragment = new EventEditorFragment();
                fragment.setArguments(bundle);
                ft.replace(R.id.eventsContainer, fragment).addToBackStack(null).commit();
            });
        }

        @Override
        public int getItemCount() {
            if (eventList == null) {
                return 0;
            }
            return eventList.getSize();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("EventsFragment", "onResume");
        eventRepository = new EventRepository(requireContext(), Utility.getUserEmail(requireContext()));
        EventList allEvents = eventRepository.getEventsByPublished(true, "from_datetime", true);
        Log.d("EventsFragment", "allEvents size:" + allEvents.getSize());
        upcoming = new EventList();
        ongoing = new EventList();
        completed = new EventList();

        for (int i = 0; i < allEvents.getSize(); i++) {
            Event event = allEvents.getEvent(i);
            Instant startTime = event.getStartTime();
            if (startTime == null) {
                continue;
            }
            if (startTime.isAfter(Instant.now())) {
                upcoming.addEvent(event);
            } else if (startTime.isBefore(Instant.now())) {
                if (startTime.plusSeconds(event.getDuration() * 60L).isAfter(Instant.now())) {
                    ongoing.addEvent(event);
                } else {
                    completed.addEvent(0, event);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}