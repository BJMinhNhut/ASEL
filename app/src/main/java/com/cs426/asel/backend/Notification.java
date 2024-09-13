package com.cs426.asel.backend;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Calendar;

public class Notification implements Parcelable {
    private String title;
    private String content;
    private Calendar dateTime;
    private int repeatMode;
    private int minuteBefore;

    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_DAILY = 1;
    public static final int REPEAT_WEEKLY = 2;
    public static final int REPEAT_MONTHLY = 3;
    public static final int REPEAT_ANNUALLY = 4;

    public Notification(String title, String content, Calendar dateTime, int repeatMode, int minuteBefore) {
        this.title = title;
        this.content = content;
        this.dateTime = dateTime;
        this.repeatMode = repeatMode;
        assert(repeatMode >= REPEAT_NONE && repeatMode <= REPEAT_ANNUALLY);
        this.minuteBefore = minuteBefore;
    }

    protected Notification(Parcel in) {
        title = in.readString();
        content = in.readString();
        dateTime = (Calendar) in.readSerializable(); // Calendar is Serializable
        repeatMode = in.readInt();
        minuteBefore = in.readInt();
    }

    public static final Creator<Notification> CREATOR = new Creator<Notification>() {
        @Override
        public Notification createFromParcel(Parcel in) {
            return new Notification(in);
        }

        @Override
        public Notification[] newArray(int size) {
            return new Notification[size];
        }
    };

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Calendar getDateTime() {
        return dateTime;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public int getMinuteBefore() {
        return minuteBefore;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeString(title);
        parcel.writeString(content);
        parcel.writeSerializable(dateTime); // Calendar is Serializable
        parcel.writeInt(repeatMode);
        parcel.writeInt(minuteBefore);
    }

    public static int getRepeatInterval(int repeatMode) {
        switch (repeatMode) {
            case REPEAT_DAILY:
                return Calendar.DAY_OF_MONTH;
            case REPEAT_WEEKLY:
                return Calendar.WEEK_OF_YEAR;
            case REPEAT_MONTHLY:
                return Calendar.MONTH;
            case REPEAT_ANNUALLY:
                return Calendar.YEAR;
            default:
                return 0;
        }
    }
}
