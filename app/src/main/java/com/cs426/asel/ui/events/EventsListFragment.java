package com.cs426.asel.ui.events;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs426.asel.R;
import com.cs426.asel.backend.Event;
import com.cs426.asel.backend.EventList;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class EventsListFragment extends Fragment {
    private RecyclerView eventRecyclerView;
    private EventAdapter eventAdapter;
    private EventList eventList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events_list, container, false);

        eventRecyclerView = view.findViewById(R.id.eventRecyclerView);
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventRecyclerView.addItemDecoration(new SpaceItemDecoration(20));

        eventList = new EventList();
        Event event;
        for (int i = 1; i <= 20; i++) {
            int type = new Random().nextInt(3);
            event = new Event();
            event.setTitle("Event " + i);
            event.setStartTime(Instant.now().plusSeconds(new Random().nextInt(31536000)));
            event.setDuration(0);
            event.setIsAllDay(false);
            event.setLocation("Location " + i);
            event.setDescription("Description " + i);
            if (type == 0) {
                event.setDuration(new Random().nextInt(525600));
            } else if (type == 1) {

            } else {
                event.setIsAllDay(true);
            }

            eventList.addEvent(event);
        }

        eventAdapter = new EventAdapter(eventList);
        eventRecyclerView.setAdapter(eventAdapter);

        return view;
    }

    public static class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int verticalSpaceHeight;

        public SpaceItemDecoration(int verticalSpaceHeight) {
            this.verticalSpaceHeight = verticalSpaceHeight;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.bottom = verticalSpaceHeight;
        }
    }

    public static class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
        private EventList eventList;

        public EventAdapter(EventList eventList) {
            this.eventList = eventList;
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
                time = "All-day";
            } else if (duration > 0) { // Event
                LocalDateTime endDateTime = startDateTime.plusMinutes(event.getDuration());

                String endTime = timeFormatter.format(endDateTime);
                String endDay = dayFormatter.format(endDateTime);
                String endMonth = monthFormatter.format(endDateTime);

                if (startDateTime.getDayOfYear() == endDateTime.getDayOfYear() && startDateTime.getYear() == endDateTime.getYear()) { // Same day
                    time = startTime + " - " + endTime;
                } else {
                    time = startMonth + " " + startDay + ", " + startTime + " - " + endMonth + " " + endDay + ", " + endTime;

                    holder.toDate.setText("-");
                    holder.endDay.setText(endDay);
                    holder.endMonth.setText(endMonth);
                }
            } else { // Task
                time = startTime;
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
