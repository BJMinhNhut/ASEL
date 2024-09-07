package com.cs426.asel.backend;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private Context context;
    private static final String DB_NAME = "asel";
    private static final int DB_VERSION = 0;

    // SQL scripts
    // create
    private static final String CREATE_EVENTS_TABLE = "CREATE TABLE EVENTS (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, description TEXT, from_datetime TEXT, duration INTEGER, place TEXT, is_repeat INTEGER, repeat_frequency INTEGER, repeat_end TEXT, remind_time TEXT, all_day INTEGER)";
    private static final String CREATE_MAILS_TABLE = "CREATE TABLE MAILS (_id TEXT PRIMARY KEY, summary TEXT, title TEXT, sender TEXT, receiver TEXT, send_time TEXT, event_id INTEGER)";
    private static final String CREATE_TAGS_TABLE = "CREATE TABLE TAGS (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)";
    private static final String CREATE_MAIL_TAGS_TABLE = "CREATE TABLE MAIL_TAGS (mail_id INTEGER, tag_id INTEGER)";

    // drop
    private static final String DROP_EVENTS_TABLE = "DROP TABLE IF EXISTS EVENTS";
    private static final String DROP_MAILS_TABLE = "DROP TABLE IF EXISTS MAILS";
    private static final String DROP_TAGS_TABLE = "DROP TABLE IF EXISTS TAGS";
    private static final String DROP_MAIL_TAGS_TABLE = "DROP TABLE IF EXISTS MAIL_TAGS";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        updateMyDatabase(db, 0, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        updateMyDatabase(db, oldVersion, newVersion);
    }

    /**
     * Database description:
     * EVENTS(_id, title, description, from_datetime, duration, place, is_repeat, repeat_frequency, repeat_end, remind_time, all_day)
     * MAILS(_id, summary, title, sender, receiver, send_time, event_id) id is a string
     * TAGS(_id, name)
     * MAIL_TAGS(mail_id, tag_id)
     */

    private void updateMyDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 1) {
            db.execSQL(CREATE_EVENTS_TABLE);
            db.execSQL(CREATE_MAILS_TABLE);
            db.execSQL(CREATE_TAGS_TABLE);
            db.execSQL(CREATE_MAIL_TAGS_TABLE);
        }
    }

    MailList getMailList(int limit, String sortOption) {
        // retreive mail list from database
        return new MailList();
    }
}
