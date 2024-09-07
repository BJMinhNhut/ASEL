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
     * ACCOUNTS(_id, gg_sig_in, email)
     * EVENTS(_id, title, desc, from_datetime, duration, place, repeat, remind_timestamp, all_day)
     * MAILS(_id, short desc, title, evt_id, mail_from, mail_to, send_time, tags)
     */

    private void updateMyDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 1) {
            // init
        }
    }
}
