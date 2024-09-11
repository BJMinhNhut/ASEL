package com.cs426.asel.backend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import java.io.Console;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class EventRepository {
    private final DatabaseHelper dbHelper;
    private final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

    // How to use this class:
    // EventRepository eventRepository = new EventRepository(getApplicationContext(), accountViewModel.getUserEmail());
    public EventRepository(Context context, String userEmail) {
        dbHelper = new DatabaseHelper(context, userEmail);
    }

    // insert and return the id of the event
    public long insertEvent(Event event) {
        Log.println(Log.WARN, "EventRepository", "Inserting event: " + event.getTitle());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Events.COLUMN_NAME_TITLE, event.getTitle());
        values.put(DatabaseContract.Events.COLUMN_NAME_DESCRIPTION, event.getDescription());
        if (event.getStartTime() != null) {
            values.put(DatabaseContract.Events.COLUMN_NAME_FROM_DATETIME, event.getStartTime().toString());
        }
        values.put(DatabaseContract.Events.COLUMN_NAME_DURATION, event.getDuration());
        values.put(DatabaseContract.Events.COLUMN_NAME_PLACE, event.getLocation());
        values.put(DatabaseContract.Events.COLUMN_NAME_IS_REPEAT, event.isRepeating() ? 1 : 0);
        values.put(DatabaseContract.Events.COLUMN_NAME_REPEAT_FREQUENCY, event.getRepeatFrequency());
        if (event.getRepeatEndDate() != null) {
            values.put(DatabaseContract.Events.COLUMN_NAME_REPEAT_END, event.getRepeatEndDate().toString());
        }
        if (event.getReminderTime() != null) {
            values.put(DatabaseContract.Events.COLUMN_NAME_REMIND_TIME, event.getReminderTime().toString());
        }
        values.put(DatabaseContract.Events.COLUMN_NAME_ALL_DAY, event.isAllDay() ? 1 : 0);
        values.put(DatabaseContract.Events.COLUMN_NAME_PUBLISHED, event.isPublished() ? 1 : 0);

        return db.insert(DatabaseContract.Events.TABLE_NAME, null, values);
    }

    public int deleteEvent(long eventId) {
        Log.println(Log.WARN, "EventRepository", "Deleting event: " + eventId);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = DatabaseContract.Events._ID + " = ?";
        String[] selectionArgs = { String.valueOf(eventId) };
        return db.delete(DatabaseContract.Events.TABLE_NAME, selection, selectionArgs);
    }

    public EventList getAllEvents() {
        Log.println(Log.INFO, "EventRepository", "Getting all events");
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        queryBuilder.setTables(DatabaseContract.Events.TABLE_NAME +
                " LEFT JOIN " +
                DatabaseContract.Mails.TABLE_NAME +
                " ON " +
                DatabaseContract.Events.TABLE_NAME + "." + DatabaseContract.Events._ID +
                " = " +
                DatabaseContract.Mails.TABLE_NAME + "." + DatabaseContract.Mails.COLUMN_NAME_EVENT_ID);

        String[] projection = {
                DatabaseContract.Events._ID,
                DatabaseContract.Events.COLUMN_NAME_TITLE,
                DatabaseContract.Events.COLUMN_NAME_DESCRIPTION,
                DatabaseContract.Events.COLUMN_NAME_FROM_DATETIME,
                DatabaseContract.Events.COLUMN_NAME_DURATION,
                DatabaseContract.Events.COLUMN_NAME_PLACE,
                DatabaseContract.Events.COLUMN_NAME_IS_REPEAT,
                DatabaseContract.Events.COLUMN_NAME_REPEAT_FREQUENCY,
                DatabaseContract.Events.COLUMN_NAME_REPEAT_END,
                DatabaseContract.Events.COLUMN_NAME_REMIND_TIME,
                DatabaseContract.Events.COLUMN_NAME_ALL_DAY,
                DatabaseContract.Mails._ID
        };

        Cursor cursor = queryBuilder.query(db, projection, null, null, null, null, null);

        EventList events = new EventList();
        while (cursor.moveToNext()) {
            Event event = getEventByCursor(cursor);
            events.addEvent(event);
        }
        cursor.close();

        return events;
    }

    // should use this for the EventFragment instead of getAllEvents()
    public EventList getEventsByPublished(boolean published) {
        Log.println(Log.INFO, "EventRepository", "Getting events by published: " + published);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        queryBuilder.setTables(DatabaseContract.Events.TABLE_NAME +
                " LEFT JOIN " +
                DatabaseContract.Mails.TABLE_NAME +
                " ON " +
                DatabaseContract.Events.TABLE_NAME + "." + DatabaseContract.Events._ID +
                " = " +
                DatabaseContract.Mails.TABLE_NAME + "." + DatabaseContract.Mails.COLUMN_NAME_EVENT_ID);

        String[] projection = {
                DatabaseContract.Events._ID,
                DatabaseContract.Events.COLUMN_NAME_TITLE,
                DatabaseContract.Events.COLUMN_NAME_DESCRIPTION,
                DatabaseContract.Events.COLUMN_NAME_FROM_DATETIME,
                DatabaseContract.Events.COLUMN_NAME_DURATION,
                DatabaseContract.Events.COLUMN_NAME_PLACE,
                DatabaseContract.Events.COLUMN_NAME_IS_REPEAT,
                DatabaseContract.Events.COLUMN_NAME_REPEAT_FREQUENCY,
                DatabaseContract.Events.COLUMN_NAME_REPEAT_END,
                DatabaseContract.Events.COLUMN_NAME_REMIND_TIME,
                DatabaseContract.Events.COLUMN_NAME_ALL_DAY,
                DatabaseContract.Mails._ID
        };

        String selection = DatabaseContract.Events.COLUMN_NAME_PUBLISHED + " = ?";
        String[] selectionArgs = { published ? "1" : "0" };

        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, null);

        EventList events = new EventList();
        while (cursor.moveToNext()) {
            Event event = getEventByCursor(cursor);
            events.addEvent(event);
        }
        cursor.close();

        return events;
    }

    private Event getEventByCursor(Cursor cursor) {
        int eventId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Events._ID));
        String mailId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Mails._ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_TITLE));
        String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_DESCRIPTION));

        String fromDatetimeString = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_FROM_DATETIME));
        Instant fromDatetime = fromDatetimeString != null ? Instant.parse(fromDatetimeString) : null;

        int duration = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_DURATION));
        String place = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_PLACE));
        boolean isRepeat = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_IS_REPEAT)) == 1;
        String repeatFrequency = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_REPEAT_FREQUENCY));

        String repeatEndString = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_REPEAT_END));
        Instant repeatEnd = repeatEndString != null ? Instant.parse(repeatEndString) : null;

        String remindTimeString = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_REMIND_TIME));
        Instant remindTime = remindTimeString != null ? Instant.parse(remindTimeString) : null;

        boolean allDay = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_ALL_DAY)) == 1;
        boolean isPublished = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_PUBLISHED)) == 1;

        return new Event(eventId, mailId, title, fromDatetime, duration, place, isRepeat, repeatFrequency, repeatEnd, remindTime, description, allDay, isPublished);
    }

}