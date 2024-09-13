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

    private final String JOIN_MAIL_ID = "mail_id";
    private final String JOIN_EVENT_ID = "evt_id";
    private final String JOIN_EVENT_TITLE = "evt_title";
    // change to string for raw query
    private final String eventProjection = COLUMN_EVENT_ID + " as " + JOIN_EVENT_ID + ", " +
            COLUMN_MAIL_ID + " as " + JOIN_MAIL_ID + ", " +
            COLUMN_EVENT_TITLE + " as " + JOIN_EVENT_TITLE + ", " +
            DatabaseContract.Events.COLUMN_NAME_DESCRIPTION + ", " +
            DatabaseContract.Events.COLUMN_NAME_FROM_DATETIME + ", " +
            DatabaseContract.Events.COLUMN_NAME_DURATION + ", " +
            DatabaseContract.Events.COLUMN_NAME_PLACE + ", " +
            DatabaseContract.Events.COLUMN_NAME_IS_REPEAT + ", " +
            DatabaseContract.Events.COLUMN_NAME_REPEAT_FREQUENCY + ", " +
            DatabaseContract.Events.COLUMN_NAME_REPEAT_END + ", " +
            DatabaseContract.Events.COLUMN_NAME_REMIND_TIME + ", " +
            DatabaseContract.Events.COLUMN_NAME_ALL_DAY + ", " +
            DatabaseContract.Events.COLUMN_NAME_PUBLISHED;

    // How to use this class:
    // EventRepository eventRepository = new EventRepository(getApplicationContext(), accountViewModel.getUserEmail());
    public EventRepository(Context context, String userEmail) {
        dbHelper = new DatabaseHelper(context, userEmail);
    }

    // insert and return the id of the event
    public long insertEvent(Event event) {
        Log.println(Log.WARN, "EventRepository", "Inserting event: " + event.getTitle());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = -1;
        db.beginTransaction();
        try {
            ContentValues values = getContentValues(event);
            id = db.insert(DatabaseContract.Events.TABLE_NAME, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("EventRepository", "Error inserting event: " + event.getTitle(), e);
        } finally {
            db.endTransaction();
        }
        return id;
    }

    public int deleteEvent(long eventId) {
        Log.println(Log.WARN, "EventRepository", "Deleting event: " + eventId);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = 0;
        db.beginTransaction();
        try {
            String selection = DatabaseContract.Events._ID + " = ?";
            String[] selectionArgs = { String.valueOf(eventId) };
            rowsDeleted = db.delete(DatabaseContract.Events.TABLE_NAME, selection, selectionArgs);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("EventRepository", "Error deleting event: " + eventId, e);
        } finally {
            db.endTransaction();
        }
        return rowsDeleted;
    }

    public EventList getAllEvents(String sortBy, boolean ascending) {
        Log.println(Log.INFO, "EventRepository", "Getting all events");
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sortOrder = null;
        if (!sortBy.isEmpty()) sortOrder = sortBy + (ascending ? " ASC" : " DESC");

        Cursor cursor = null;
        EventList events = new EventList();
        String query = "SELECT " + eventProjection + " FROM " + EVENT_MAIL_JOIN +
                (sortOrder != null ? " ORDER BY " + sortOrder : "");

        Log.d("EventRepository", "Generated SQL query for getAllEvents: " + query);
        try {
            cursor = db.rawQuery(query, null);
            while (cursor.moveToNext()) {
                Event event = getEventByCursor(cursor);
                events.addEvent(event);
            }
        } catch(Exception e) {
            Log.e("EventRepository", "Error getting all events", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return events;
    }

    // should use this for the EventFragment instead of getAllEvents()
    public EventList getEventsByPublished(boolean published, String sortBy, boolean ascending) {
        Log.println(Log.INFO, "EventRepository", "Getting events by published: " + published);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sortOrder = null;
        if (!sortBy.isEmpty()) sortOrder = sortBy + (ascending ? " ASC" : " DESC");

        Cursor cursor = null;
        EventList events = new EventList();
        String query = "SELECT " + eventProjection + " FROM " + EVENT_MAIL_JOIN +
                " WHERE " + DatabaseContract.Events.COLUMN_NAME_PUBLISHED + " = " + (published ? 1 : 0)
                + (sortOrder != null ? " ORDER BY " + sortOrder : "");
        Log.d("EventRepository", "Generated SQL query for getEventsByPublished: " + query);
        try {
            cursor = db.rawQuery(query, null);
            while (cursor.moveToNext()) {
                Event event = getEventByCursor(cursor);
                events.addEvent(event);
            }
        } catch(Exception e) {
            Log.e("EventRepository", "Error getting events by published: " + published, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return events;
    }

    // add event to calendar -> publish true, remove event from calendar -> publish false
    public int setPublishEvent(long eventId, boolean published) {
        Log.println(Log.WARN, "EventRepository", "Publishing event: " + eventId);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsUpdated = 0;
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseContract.Events.COLUMN_NAME_PUBLISHED, published ? 1 : 0);
            String selection = DatabaseContract.Events._ID + " = ?";
            String[] selectionArgs = { String.valueOf(eventId) };
            rowsUpdated = db.update(DatabaseContract.Events.TABLE_NAME, values, selection, selectionArgs);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("EventRepository", "Error publishing event: " + eventId, e);
        } finally {
            db.endTransaction();
        }
        return rowsUpdated;
    }

    public int updateEvent(Event event) {
        Log.println(Log.WARN, "EventRepository", "Updating event: " + event.getTitle());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsUpdated = 0;
        db.beginTransaction();
        try {
            ContentValues values = getContentValues(event);
            String selection = DatabaseContract.Events._ID + " = ?";
            String[] selectionArgs = { String.valueOf(event.getID()) };
            rowsUpdated = db.update(DatabaseContract.Events.TABLE_NAME, values, selection, selectionArgs);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("EventRepository", "Error updating event: " + event.getTitle(), e);
        } finally {
            db.endTransaction();
        }
        return rowsUpdated;
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
        int eventId = cursor.getInt(cursor.getColumnIndexOrThrow(JOIN_EVENT_ID));
        String mailId = cursor.getString(cursor.getColumnIndexOrThrow(JOIN_MAIL_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(JOIN_EVENT_TITLE));
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

        Event event = new Event(eventId, mailId, title, fromDatetime, duration, place, isRepeat, repeatFrequency, repeatEnd, remindTime, description, allDay, isPublished);
        Log.d("EventRepository", "Retrieved event: " + event.getTitle() + " with ID: " + event.getID() + " mail ID: " + event.getMailID());
        return event;
    }

}