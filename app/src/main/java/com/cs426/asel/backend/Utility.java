package com.cs426.asel.backend;
import static android.content.Context.MODE_PRIVATE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.cs426.asel.backend.NotiWorker;
import com.cs426.asel.backend.Notification;
import com.cs426.asel.backend.NotificationReceiver;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Locale;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public final class Utility {

    // Unique tags for the scheduled work
    private static final String PERIODIC_WORK_TAG = "NotiPeriodicWorker";
    private static final String IMMEDIATE_WORK_TAG = "NotiImmediateWorker";

    public static String getUserEmail(Context context) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            return null;
        }
        return account.getEmail();
    }

    public static String parseInstant(Instant instant, String pattern) {
        return instant.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern(pattern));
    }

    public static Instant parseToInstant(String dateTime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
        TemporalAccessor temporalAccessor = formatter.parse(dateTime);

        // Check if the parsed date-time string contains a zone or offset
        if (temporalAccessor.query(TemporalQueries.zoneId()) == null && temporalAccessor.query(TemporalQueries.offset()) == null) {
            // If no zone or offset is found, use the system default zone
            LocalDateTime localDateTime = LocalDateTime.from(temporalAccessor);
            return localDateTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        } else {
            // If a zone or offset is found, parse as ZonedDateTime
            ZonedDateTime zonedDateTime = ZonedDateTime.from(temporalAccessor);
            return zonedDateTime.toInstant();
        }
    }

    public static void scheduleNotification(Context context, int eventId, Notification noti) {
        // Create an intent to trigger NotificationReceiver
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification", noti);
        intent.putExtra("event_id", eventId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, eventId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar eventDateTime = noti.getDateTime();
        int minuteBefore = noti.getMinuteBefore();

        long triggerTime = eventDateTime.getTimeInMillis() - (minuteBefore * 60 * 1000);

        // Set up the AlarmManager to trigger at the specified time
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {  // API 31+
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        // Optionally save the notification information using SharedPreferences for future reference
        SharedPreferences prefs = context.getSharedPreferences("notifications", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("event_" + eventId, eventId);
        editor.apply();
    }

    // Method to cancel a scheduled notification
    public static void cancelNotification(Context context, int eventId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, eventId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        // Also remove the saved notification data
        SharedPreferences prefs = context.getSharedPreferences("notifications", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("event_" + eventId);
        editor.apply();
    }

    // Method to update a scheduled notification
    public static void updateNotification(Context context, int eventId, Notification noti) {
        // Cancel the old notification
        cancelNotification(context, eventId);

        // Schedule a new notification with updated time
        scheduleNotification(context, eventId, noti);
    }

    // Static method to start a new scheduled work
    public static void startScheduledWork(Context context) {
        // Create a OneTimeWorkRequest for immediate execution
        OneTimeWorkRequest immediateWorkRequest = new OneTimeWorkRequest.Builder(NotiWorker.class)
                .addTag(IMMEDIATE_WORK_TAG)  // Use a separate tag for immediate work
                .build();

        // Create a PeriodicWorkRequest that runs every 1 hour
        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(NotiWorker.class, 1, TimeUnit.HOURS)
                .addTag(PERIODIC_WORK_TAG)  // Use a unique tag for periodic work
                .build();

        // Schedule the one-time work to run immediately
        WorkManager.getInstance(context).enqueue(immediateWorkRequest);

        // Schedule the periodic work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG, // Unique name for the periodic work
                androidx.work.ExistingPeriodicWorkPolicy.REPLACE, // If work exists, replace it
                periodicWorkRequest
        );
    }

    // Static method to stop the scheduled work
    public static void stopScheduledWork(Context context) {
        // Cancel all work by the unique tag
        WorkManager.getInstance(context).cancelAllWorkByTag(PERIODIC_WORK_TAG);
        WorkManager.getInstance(context).cancelAllWorkByTag(IMMEDIATE_WORK_TAG);
    }
}
