package com.cs426.asel.backend;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "ASEL.db";
    private static final int DB_VERSION = 1;
    private Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * Database description:
     * EVENTS(_id, title, description, from_datetime, duration, place, is_repeat, repeat_frequency, repeat_end, remind_time, all_day)
     * MAILS(_id, summary, title, sender, receiver, send_time, event_id) id is a string
     * TAGS(_id, name)
     * MAIL_TAGS(mail_id, tag_id)
     */

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DatabaseContract.Mails.CREATE_TABLE);
        db.execSQL(DatabaseContract.Events.CREATE_TABLE);
        db.execSQL(DatabaseContract.Tags.CREATE_TABLE);
        db.execSQL(DatabaseContract.MailTags.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DatabaseContract.Mails.DROP_TABLE);
        db.execSQL(DatabaseContract.Events.DROP_TABLE);
        db.execSQL(DatabaseContract.Tags.DROP_TABLE);
        db.execSQL(DatabaseContract.MailTags.DROP_TABLE);
        onCreate(db);
    }
}
