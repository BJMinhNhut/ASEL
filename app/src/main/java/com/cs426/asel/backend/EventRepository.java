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
    private final String COLUMN_MAIL_ID = DatabaseContract.Mails.TABLE_NAME + "." + DatabaseContract.Mails._ID;
    private final String COLUMN_EVENT_ID = DatabaseContract.Events.TABLE_NAME + "." + DatabaseContract.Events._ID;
    private final String COLUMN_EVENT_TITLE = DatabaseContract.Events.TABLE_NAME + "." + DatabaseContract.Events.COLUMN_NAME_TITLE;
    final String EVENT_MAIL_JOIN = DatabaseContract.Events.TABLE_NAME +
            " LEFT JOIN " + DatabaseContract.Mails.TABLE_NAME +
            " ON " + COLUMN_EVENT_ID +
            " = " + DatabaseContract.Mails.TABLE_NAME + "." + DatabaseContract.Mails.COLUMN_NAME_EVENT_ID;

    private final String[] eventProjection = {
            COLUMN_EVENT_ID,
            COLUMN_EVENT_TITLE,
            DatabaseContract.Events.COLUMN_NAME_DESCRIPTION,
            DatabaseContract.Events.COLUMN_NAME_FROM_DATETIME,
            DatabaseContract.Events.COLUMN_NAME_DURATION,
            DatabaseContract.Events.COLUMN_NAME_PLACE,
            DatabaseContract.Events.COLUMN_NAME_IS_REPEAT,
            DatabaseContract.Events.COLUMN_NAME_REPEAT_FREQUENCY,
            DatabaseContract.Events.COLUMN_NAME_REPEAT_END,
            DatabaseContract.Events.COLUMN_NAME_REMIND_TIME,
            DatabaseContract.Events.COLUMN_NAME_ALL_DAY,
            DatabaseContract.Events.COLUMN_NAME_PUBLISHED,
            COLUMN_MAIL_ID
    };

    // How to use this class:
    // EventRepository eventRepository = new EventRepository(getApplicationContext(), accountViewModel.getUserEmail());
    public EventRepository(Context context, String userEmail) {
        dbHelper = new DatabaseHelper(context, userEmail);
    }

    // insert and return the id of the event
    public long insertEvent(Event event) {
        Log.println(Log.WARN, "EventRepository", "Inserting event: " + event.getTitle());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = getContentValues(event);
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
        queryBuilder.setTables(EVENT_MAIL_JOIN);

        Cursor cursor = queryBuilder.query(db, eventProjection, null, null, null, null, null);

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
        queryBuilder.setTables(EVENT_MAIL_JOIN);

        String selection = DatabaseContract.Events.COLUMN_NAME_PUBLISHED + " = ?";
        String[] selectionArgs = { published ? "1" : "0" };

        Cursor cursor = queryBuilder.query(db, eventProjection, selection, selectionArgs, null, null, null);

        EventList events = new EventList();
        while (cursor.moveToNext()) {
            Event event = getEventByCursor(cursor);
            events.addEvent(event);
        }
        cursor.close();

        return events;
    }

    // add event to calendar -> publish true, remove event from calendar -> publish false
    public int setPublishEvent(long eventId, boolean published) {
        Log.println(Log.WARN, "EventRepository", "Publishing event: " + eventId);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Events.COLUMN_NAME_PUBLISHED, published ? 1 : 0);
        String selection = DatabaseContract.Events._ID + " = ?";
        String[] selectionArgs = { String.valueOf(eventId) };
        return db.update(DatabaseContract.Events.TABLE_NAME, values, selection, selectionArgs);
    }

    public int updateEvent(Event event) {
        Log.println(Log.WARN, "EventRepository", "Updating event: " + event.getTitle());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = getContentValues(event);
        String selection = DatabaseContract.Events._ID + " = ?";
        String[] selectionArgs = { String.valueOf(event.getID()) };
        return db.update(DatabaseContract.Events.TABLE_NAME, values, selection, selectionArgs);
    }

    private ContentValues getContentValues(Event event) {
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
        return values;
    }

    private Event getEventByCursor(Cursor cursor) {
        int eventId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EVENT_ID));
        String mailId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MAIL_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TITLE));
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