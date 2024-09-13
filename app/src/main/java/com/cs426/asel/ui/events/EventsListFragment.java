package com.cs426.asel.ui.events;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs426.asel.R;
import com.cs426.asel.backend.Event;
import com.cs426.asel.backend.EventList;
import com.cs426.asel.backend.EventRepository;
import com.cs426.asel.backend.Utility;
import com.cs426.asel.databinding.FragmentEventsBinding;
import com.cs426.asel.databinding.FragmentEventsListBinding;
import com.cs426.asel.ui.account.EventEditorFragment;
import com.cs426.asel.ui.decoration.SpaceItemDecoration;
import com.google.android.material.tabs.TabLayout;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class EventsListFragment extends Fragment {
    private FragmentEventsListBinding binding;
    private RecyclerView eventRecyclerView;
    private EventAdapter eventAdapter;
    private EventList ongoing, upcoming, completed;
    private EventRepository eventRepository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events_list, container, false);
        binding = FragmentEventsListBinding.inflate(inflater, container, false);

        eventRecyclerView = view.findViewById(R.id.eventRecyclerView);
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventRecyclerView.addItemDecoration(new SpaceItemDecoration(20));

        eventRepository = new EventRepository(requireContext(), Utility.getUserEmail(requireContext()));
        EventList allEvents = eventRepository.getEventsByPublished(true);
        for (int i = 0; i < allEvents.getSize(); i++) {
            Event event = allEvents.getEvent(i);
            if (event.getStartTime().isAfter(Instant.now())) {
                upcoming.addEvent(event);
            } else if (event.getStartTime().isBefore(Instant.now())) {
                if (event.getStartTime().plusSeconds(event.getDuration() * 60).isAfter(Instant.now())) {
                    ongoing.addEvent(event);
                } else {
                    completed.addEvent(event);
                }
            }
        }

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
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
//        binding.newEventFab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
//                ft.replace(R.id.emailsContainer, new EventEditorFragment()).addToBackStack(null).commit();
//            }
//        });

        return view;
    }

    public static class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
        private EventList eventList;

        public EventAdapter(EventList eventList) {
            this.eventList = eventList;
        }

        public void setEventList(EventList newEventList) {
            eventList = newEventList;
            notifyDataSetChanged();
        }

        public static class EventViewHolder extends RecyclerView.ViewHolder {
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
        public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
            return new EventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
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
        }

        @Override
        public int getItemCount() {
            return eventList.getSize();
        }
    }
}
