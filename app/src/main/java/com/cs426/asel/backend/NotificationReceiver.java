package com.cs426.asel.backend;
import static com.cs426.asel.backend.Utility.scheduleNotification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.cs426.asel.R;

import java.util.Calendar;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NotificationReceiver", "Notification received");
        Toast.makeText(context, "Notification received", Toast.LENGTH_SHORT).show();
        Notification noti = intent.getParcelableExtra("notification");
        int eventId = intent.getIntExtra("event_id", -1);
        if (noti == null) {
            return;
        }
        String eventTitle = noti.getTitle();
        String eventContent = noti.getContent();
        Calendar eventDateTime = noti.getDateTime();
        int repeatMode = noti.getRepeatMode();
        Calendar reminderTime = noti.getReminderTime();

        // Build and display the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getString(R.string.noti_channel_id))
                .setSmallIcon(R.drawable.notification_icon) // Set your notification icon
                .setContentTitle(eventTitle)
                .setContentText(eventContent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true); // Automatically remove the notification when clicked

        // Create a notification manager and display the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(eventId, builder.build());
        }

        // Reschedule the notification for the next day (or any interval you choose)
        Calendar nextScheduledTime = eventDateTime;
        Calendar nextTriggerTime = reminderTime;
        if (repeatMode != Notification.REPEAT_NONE) {
            nextScheduledTime.add(Notification.getRepeatInterval(repeatMode), 1);
            nextTriggerTime.add(Notification.getRepeatInterval(repeatMode), 1);

            Notification newNoti = new Notification(eventTitle, eventContent, nextScheduledTime, repeatMode, nextTriggerTime);

            // Call scheduleNotification to reschedule for the next day
            scheduleNotification(context, eventId, newNoti);
        }
    }
}
