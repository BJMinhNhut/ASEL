package com.cs426.asel.backend;

import android.provider.BaseColumns;

/**
 * Database description:
 * EVENTS(_id, title, description, from_datetime, duration, place, is_repeat, repeat_frequency, repeat_end, remind_time, all_day)
 * MAILS(_id, summary, title, sender, receiver, send_time, event_id) id is a string
 * TAGS(_id, name)
 * MAIL_TAGS(mail_id, tag_id)
 */

public final class DatabaseContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private DatabaseContract() {}

    /* Inner class that defines the table contents */
    public static class Mails implements BaseColumns {
        public static final String TABLE_NAME = "MAILS";
        public static final String COLUMN_NAME_SUMMARY = "summary";
        public static final String COLUMN_NAME_CONTENT = "content";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_SENDER = "sender";
        public static final String COLUMN_NAME_RECEIVER = "receiver";
        public static final String COLUMN_NAME_SEND_TIME = "send_time";
        public static final String COLUMN_NAME_EVENT_ID = "event_id";
        public static final String COLUMN_NAME_IS_READ = "is_read";

        // SQL scripts
        public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " TEXT PRIMARY KEY," +
                COLUMN_NAME_SUMMARY + " TEXT," +
                COLUMN_NAME_CONTENT + " TEXT," +
                COLUMN_NAME_TITLE + " TEXT," +
                COLUMN_NAME_SENDER + " TEXT," +
                COLUMN_NAME_RECEIVER + " TEXT," +
                COLUMN_NAME_SEND_TIME + " TEXT," +
                COLUMN_NAME_EVENT_ID + " INTEGER," +
                COLUMN_NAME_IS_READ + " INTEGER)";

        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static class Events implements BaseColumns {
        public static final String TABLE_NAME = "EVENTS";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_FROM_DATETIME = "from_datetime";
        public static final String COLUMN_NAME_DURATION = "duration";
        public static final String COLUMN_NAME_PLACE = "place";
        public static final String COLUMN_NAME_IS_REPEAT = "is_repeat";
        public static final String COLUMN_NAME_REPEAT_FREQUENCY = "repeat_frequency";
        public static final String COLUMN_NAME_REPEAT_END = "repeat_end";
        public static final String COLUMN_NAME_REMIND_TIME = "remind_time";
        public static final String COLUMN_NAME_ALL_DAY = "all_day";

        // SQL scripts
        public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_NAME_TITLE + " TEXT," +
                COLUMN_NAME_DESCRIPTION + " TEXT," +
                COLUMN_NAME_FROM_DATETIME + " TEXT," +
                COLUMN_NAME_DURATION + " INTEGER," +
                COLUMN_NAME_PLACE + " TEXT," +
                COLUMN_NAME_IS_REPEAT + " INTEGER," +
                COLUMN_NAME_REPEAT_FREQUENCY + " TEXT," +
                COLUMN_NAME_REPEAT_END + " TEXT," +
                COLUMN_NAME_REMIND_TIME + " TEXT," +
                COLUMN_NAME_ALL_DAY + " INTEGER)";

        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static class Tags implements BaseColumns {
        public static final String TABLE_NAME = "TAGS";
        public static final String COLUMN_NAME_NAME = "name";

        // SQL scripts
        public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_NAME_NAME + " TEXT)";

        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static class MailTags implements BaseColumns {
        public static final String TABLE_NAME = "MAIL_TAGS";
        public static final String COLUMN_NAME_MAIL_ID = "mail_id";
        public static final String COLUMN_NAME_TAG_ID = "tag_id";

        // SQL scripts
        public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_NAME_MAIL_ID + " INTEGER," +
                COLUMN_NAME_TAG_ID + " INTEGER)";

        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

}
