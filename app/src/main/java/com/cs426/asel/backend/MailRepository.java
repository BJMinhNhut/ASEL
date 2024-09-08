package com.cs426.asel.backend;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class MailRepository {
    private final DatabaseHelper dbHelper;

    public MailRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

//    public long insertMail(Mail mail) {
//        Log.println(Log.INFO, "MailRepository", "Inserting mail: " + mail.getSubject());
//        SQLiteDatabase db = dbHelper.getWritableDatabase();
//        ContentValues values = new ContentValues();
//        values.put(DatabaseContract.Mails.COLUMN_NAME_TITLE, mail.getTitle());
//        values.put(DatabaseContract.Mails.COLUMN_NAME_BODY, mail.getBody());
//        values.put(DatabaseContract.Mails.COLUMN_NAME_FROM, mail.getFrom());
//        values.put(DatabaseContract.Mails.COLUMN_NAME_TO, mail.getTo());
//        values.put(DatabaseContract.Mails.COLUMN_NAME_CC, mail.getCc());
//        values.put(DatabaseContract.Mails.COLUMN_NAME_BCC, mail.getBcc());
//        values.put(DatabaseContract.Mails.COLUMN_NAME_DATE, mail.getDate().toString());
//        values.put(DatabaseContract.Mails.COLUMN_NAME_EVENT_ID, mail.getEventID());
//
//        return db.insert(DatabaseContract.Mails.TABLE_NAME, null, values);
//    }
//
//    public int deleteMail(long mailId) {
//        Log.println(Log.INFO, "MailRepository", "Deleting mail: " + mailId);
//        SQLiteDatabase db = dbHelper.getWritableDatabase();
//        String selection = DatabaseContract.Mails._ID + " = ?";
//        String[] selectionArgs = { String.valueOf(mailId) };
//        return db.delete(DatabaseContract.Mails.TABLE_NAME, selection, selectionArgs);
//    }
//
//    public MailList getAllMails() {
//        Log.println(Log.INFO, "MailRepository", "Getting all mails");
//        SQLiteDatabase db = dbHelper.getReadableDatabase();
//        queryBuilder.setTables(DatabaseContract.Mails.TABLE_NAME +
//                " LEFT JOIN " +
//                DatabaseContract.Events.TABLE_NAME +
//                " ON " +
//                DatabaseContract.Mails.TABLE_NAME + "." + DatabaseContract.Mails.COLUMN_NAME_EVENT_ID +
//                " = " +
//                DatabaseContract.Events.TABLE_NAME + "." + DatabaseContract.Events._ID);
//        Cursor cursor = queryBuilder.query(db, null, null, null, null, null, null);
//        MailList mailList = new MailList();
//        while (cursor.moveToNext()) {
//            Mail mail = new Mail();
//            mail.setID(cursor.getInt(cursor.getColumnIndex(DatabaseContract.Mails._ID)));
//            mail.setSubject(cursor.getString(cursor.getColumnIndex(DatabaseContract.Mails.COLUMN_NAME_SUBJECT));
//            mail.setBody(cursor.getString(cursor.getColumnIndex(DatabaseContract.Mails.COLUMN_NAME_BODY));
//            mail.setFrom(cursor.getString(cursor.getColumnIndex(DatabaseContract.Mails.COLUMN_NAME_SENDER));
//            mail.setTo(cursor.getString(cursor.getColumnIndex(DatabaseContract.Mails.COLUMN_NAME_RECEIVER));

}
