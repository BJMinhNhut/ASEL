package com.cs426.asel.backend;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private Context context;
    private static final String DB_NAME = "asel";
    private static final int DB_VERSION = 0;

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
     * EVENTS(_id, title, desc, from_datetime, duration, place, is_repeat, repeat_frequency, repeat_end, remind_time, all_day)
     * MAILS(_id, summary, title, sender, receiver, send_time, event_id)
     * TAGS(_id, name)
     * MAIL_TAGS(mail_id, tag_id)
     */

    private void updateMyDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 1) {
            db.execSQL("CREATE TABLE EVENTS (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, desc TEXT, from_datetime TEXT, duration INTEGER, place TEXT, is_repeat INTEGER, repeat_frequency INTEGER, repeat_end TEXT, remind_time TEXT, all_day INTEGER)");
            db.execSQL("CREATE TABLE MAILS (_id INTEGER PRIMARY KEY AUTOINCREMENT, summary TEXT, title TEXT, sender TEXT, receiver TEXT, send_time TEXT, event_id INTEGER)");
            db.execSQL("CREATE TABLE TAGS (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");
            db.execSQL("CREATE TABLE MAIL_TAGS (mail_id INTEGER, tag_id INTEGER)");
        }
    }
}
