package com.cs426.asel.backend;

import androidx.annotation.NonNull;

import java.time.Instant;

public class Event {
    int mID;
    String mMailID;
    String mTitle;
    Instant mStartTime;
    int mDuration; // in minutes, duration = 0 if the event is a task
    String mLocation;

    // Repeat information
    boolean mIsRepeating;
    String mRepeatFrequency; // e.g., "daily", "weekly", "monthly", "yearly"
    // TODO: weekly, monthly, yearly customizations
    Instant mRepeatEndDate;

    Instant mReminderTime;
    String mDescription;

    boolean mIsAllDay;
    boolean mIsPublished;

    public Event() {
        mID = -1; // -1 for undefined ID (not in the database)
        mMailID = "";
        mTitle = "";
        mStartTime = Instant.now();
        mDuration = 0;
        mLocation = "";
        mIsRepeating = false;
        mRepeatFrequency = "";
        mRepeatEndDate = Instant.now();
        mReminderTime = Instant.now();
        mDescription = "";
        mIsAllDay = false;
        mIsPublished = false;
    }

    public Event(int id, String mailID, String title, Instant startTime, int duration, String location, boolean isRepeating, String repeatFrequency, Instant repeatEndDate, Instant reminderTime, String description, boolean isAllDay, boolean isPublished) {
        mID = id;
        mMailID = mailID;
        mTitle = title;
        mStartTime = startTime;
        mDuration = duration;
        mLocation = location;
        mIsRepeating = isRepeating;
        mRepeatFrequency = repeatFrequency;
        mRepeatEndDate = repeatEndDate;
        mReminderTime = reminderTime;
        mDescription = description;
        mIsAllDay = isAllDay;
        mIsPublished = isPublished;
    }

    public Event(String mailID, String title, Instant startTime, int duration, String location, boolean isRepeating, String repeatFrequency, Instant repeatEndDate, Instant reminderTime, String description, boolean isAllDay, boolean isPublished) {
        mID = -1; // -1 for undefined ID (not in the database)
        mMailID = mailID;
        mTitle = title;
        mStartTime = startTime;
        mDuration = duration;
        mLocation = location;
        mIsRepeating = isRepeating;
        mRepeatFrequency = repeatFrequency;
        mRepeatEndDate = repeatEndDate;
        mReminderTime = reminderTime;
        mDescription = description;
        mIsAllDay = isAllDay;
        mIsPublished = isPublished;
    }

    public int getID() {
        return mID;
    }

    public String getMailID() {
        return mMailID;
    }

    public String getTitle() {
        return mTitle;
    }

    public Instant getStartTime() {
        return mStartTime;
    }

    public int getDuration() {
        return mDuration;
    }

    public String getLocation() {
        return mLocation;
    }

    public boolean isRepeating() {
        return mIsRepeating;
    }

    public String getRepeatFrequency() {
        return mRepeatFrequency;
    }

    public Instant getRepeatEndDate() {
        return mRepeatEndDate;
    }

    public Instant getReminderTime() {
        return mReminderTime;
    }

    public String getDescription() {
        return mDescription;
    }

    public boolean isAllDay() {
        return mIsAllDay;
    }

    public boolean isPublished() {
        return mIsPublished;
    }

    public void setMailID(String mailID) {
        mMailID = mailID;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setStartTime(Instant startTime) {
        mStartTime = startTime;
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    public void setLocation(String location) {
        mLocation = location;
    }

    public void setIsRepeating(boolean isRepeating) {
        mIsRepeating = isRepeating;
    }

    public void setRepeatFrequency(String repeatFrequency) {
        // assert repeatFrequency is one of "daily", "weekly", "monthly", "yearly"
        assert repeatFrequency.equals("daily") || repeatFrequency.equals("weekly") || repeatFrequency.equals("monthly") || repeatFrequency.equals("yearly");
        mRepeatFrequency = repeatFrequency;
    }

    public void setRepeatEndDate(Instant repeatEndDate) {
        mRepeatEndDate = repeatEndDate;
    }

    public void setReminderTime(Instant reminderTime) {
        mReminderTime = reminderTime;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public void setIsAllDay(boolean isAllDay) {
        mIsAllDay = isAllDay;
    }

    public void setIsPublished(boolean isPublished) {
        mIsPublished = isPublished;
    }

    @NonNull
    public String toString() {
        return "Event{" +
                "mID=" + mID +
                ", mMailID='" + mMailID + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mStartTime=" + mStartTime +
                ", mDuration=" + mDuration +
                ", mLocation='" + mLocation + '\'' +
                ", mIsRepeating=" + mIsRepeating +
                ", mRepeatFrequency='" + mRepeatFrequency + '\'' +
                ", mRepeatEndDate=" + mRepeatEndDate +
                ", mReminderTime=" + mReminderTime +
                ", mDescription='" + mDescription + '\'' +
                ", mIsAllDay=" + mIsAllDay +
                '}';
    }
}
