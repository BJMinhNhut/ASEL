package com.cs426.asel.backend;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.api.services.gmail.model.Message;

import java.util.Calendar;
import java.util.List;

public class NotiWorker extends Worker implements GmailServices.EmailCallback{
    private GmailServices gmailServices;

    public NotiWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gmailServices = new GmailServices(context, this);
    }

    @NonNull
    @Override
    public Result doWork() {
        gmailServices.fetchAllEmailIDs();
        // The task you want to run in the background
        Log.d("NotiWorker", "Running background task every hour");

        // Return success to indicate that the work completed successfully
        return Result.success();
    }

    @Override
    public void onEmailIDsFetched(List<Message> emailIDs) {
        // Call the database
        Log.d("NotiWorker", "Fetched " + emailIDs.size() + " emails");
        Context appContext = getApplicationContext();
        MailRepository mailRepository = new MailRepository(appContext, Utility.getUserEmail(appContext));
        int newMailCount = 0;
        for (Message message: emailIDs) {
            if (!mailRepository.isMailExists(message.getId())) {
                newMailCount++;
            }
        }
        Calendar currentTime = Calendar.getInstance();
        currentTime.add(Calendar.MINUTE, 1);
        Notification notification = new Notification("New Emails", newMailCount + " emails for " + Utility.getUserEmail(appContext), currentTime, Notification.REPEAT_NONE, 0);
        Utility.scheduleNotification(appContext, -1, notification);
    }

    @Override
    public void onEmailContentFetched() {

    }
}
