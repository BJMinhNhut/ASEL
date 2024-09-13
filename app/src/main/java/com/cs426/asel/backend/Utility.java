package com.cs426.asel.backend;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.cs426.asel.backend.NotificationReceiver;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

public final class Utility {
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
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
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
}
