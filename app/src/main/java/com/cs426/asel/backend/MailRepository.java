package com.cs426.asel.backend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.time.Instant;

public class MailRepository {
    private final DatabaseHelper dbHelper;

    public MailRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public long insertMail(Mail mail) {
        EventRepository eventRepository = new EventRepository(dbHelper.getContext());
        long eventId = eventRepository.insertEvent(mail.getEvent());
        if (eventId == -1) {
            Log.println(Log.ERROR, "MailRepository", "Failed to insert event for mail: " + mail.getTitle());
            return -1;
        }

        Log.println(Log.INFO, "MailRepository", "Inserting mail: " + mail.getTitle());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Mails._ID, mail.getId());
        values.put(DatabaseContract.Mails.COLUMN_NAME_TITLE, mail.getTitle());
        values.put(DatabaseContract.Mails.COLUMN_NAME_SENDER, mail.getSender());
        values.put(DatabaseContract.Mails.COLUMN_NAME_RECEIVER, mail.getReceiver());
        values.put(DatabaseContract.Mails.COLUMN_NAME_CONTENT, mail.getContent());
        values.put(DatabaseContract.Mails.COLUMN_NAME_SUMMARY, mail.getSummary());
        values.put(DatabaseContract.Mails.COLUMN_NAME_SEND_TIME, mail.getReceivedTime().toString());
        values.put(DatabaseContract.Mails.COLUMN_NAME_EVENT_ID, eventId);
        values.put(DatabaseContract.Mails.COLUMN_NAME_IS_READ, mail.isRead() ? 1 : 0);

        // NOTE: the value return is row id, not the actual id of the mail
        return db.insert(DatabaseContract.Mails.TABLE_NAME, null, values);
    }

    public int deleteMail(String mailId) {
        Log.println(Log.INFO, "MailRepository", "Deleting mail: " + mailId);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = DatabaseContract.Mails._ID + " = ?";
        String[] selectionArgs = { mailId };
        return db.delete(DatabaseContract.Mails.TABLE_NAME, selection, selectionArgs);
    }

    public MailList getAllMails(String sortBy, boolean isAscending) {
        Log.println(Log.INFO, "MailRepository", "Getting all mails");
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                DatabaseContract.Mails._ID,
                DatabaseContract.Mails.COLUMN_NAME_TITLE,
                DatabaseContract.Mails.COLUMN_NAME_CONTENT,
                DatabaseContract.Mails.COLUMN_NAME_SUMMARY,
                DatabaseContract.Mails.COLUMN_NAME_SENDER,
                DatabaseContract.Mails.COLUMN_NAME_RECEIVER,
                DatabaseContract.Mails.COLUMN_NAME_SEND_TIME,
                DatabaseContract.Mails.COLUMN_NAME_EVENT_ID,
                DatabaseContract.Mails.COLUMN_NAME_IS_READ
        };

        String sortOrder = null;
        if (!sortBy.isEmpty()) sortOrder = sortBy + (isAscending ? " ASC" : " DESC");

        Cursor cursor = db.query(DatabaseContract.Mails.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder);

        MailList mailList = new MailList();
        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Mails._ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Mails.COLUMN_NAME_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Mails.COLUMN_NAME_CONTENT));
            String summary = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Mails.COLUMN_NAME_SUMMARY));
            String sender = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Mails.COLUMN_NAME_SENDER));
            String receiver = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Mails.COLUMN_NAME_RECEIVER));
            Instant sendTime = Instant.parse(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Mails.COLUMN_NAME_SEND_TIME)));
            int eventID = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Mails.COLUMN_NAME_EVENT_ID));
            boolean isRead = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Mails.COLUMN_NAME_IS_READ)) == 1;

            Event event = getEventByID(id, eventID);
            mailList.addMail(new Mail(id, title, sender, receiver, content, summary, event, sendTime, isRead, "None"));
        }
        cursor.close();
        return mailList;
    }

    // TODO: implement this
    public MailList getMailByTags() {
        return null;
    }

    public int updateRead(String id, boolean isRead) {
        Log.println(Log.INFO, "MailRepository", "Updating mail read status: " + id);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Mails.COLUMN_NAME_IS_READ, isRead);

        String selection = DatabaseContract.Mails._ID + " = ?";
        String[] selectionArgs = { id };

        return db.update(
                DatabaseContract.Mails.TABLE_NAME,
                values,
                selection,
                selectionArgs
        );
    }

    public boolean isMailExists(String id) {
        Log.println(Log.INFO, "MailRepository", "Checking if mail exists: " + id);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = { DatabaseContract.Mails._ID };
        String selection = DatabaseContract.Mails._ID + " = ?";
        String[] selectionArgs = { id };

        Cursor cursor = db.query(
                DatabaseContract.Mails.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    private Event getEventByID(String mailId, int eventID) {
        Log.println(Log.INFO, "MailRepository", "Getting event by ID: " + eventID);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

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
                DatabaseContract.Events.COLUMN_NAME_PUBLISHED
        };

        String selection = DatabaseContract.Events._ID + " = ?";
        String[] selectionArgs = { String.valueOf(eventID) };

        Cursor cursor = db.query(
                DatabaseContract.Events.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        Event event = null;
        if (cursor.moveToNext()) {
            String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_TITLE));
            String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_DESCRIPTION));
            Instant fromDatetime = Instant.parse(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_FROM_DATETIME)));
            int duration = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_DURATION));
            String place = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_PLACE));
            boolean isRepeat = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_IS_REPEAT)) == 1;
            String repeatFrequency = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_REPEAT_FREQUENCY));
            Instant repeatEnd = Instant.parse(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_REPEAT_END)));
            Instant remindTime = Instant.parse(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_REMIND_TIME)));
            boolean allDay = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_ALL_DAY)) == 1;
            boolean isPublished = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Events.COLUMN_NAME_PUBLISHED)) == 1;

            event = new Event(eventID, mailId, title, fromDatetime, duration, place, isRepeat, repeatFrequency, repeatEnd, remindTime, description, allDay, isPublished);
        }
        cursor.close();
        return event;
    }

}
