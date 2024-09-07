package com.cs426.asel.backend;

import java.util.ArrayList;
import java.util.List;

public class EventList {
    private List<Event> mEvents;

    public EventList() {
        mEvents = new ArrayList<>();
    }

    public void addEvent(Event event) {
        mEvents.add(event);
    }

    public void removeEvent(int index) {
        mEvents.remove(index);
    }

    public Event getEvent(int index) {
        return mEvents.get(index);
    }

    public int getSize() {
        return mEvents.size();
    }
}
