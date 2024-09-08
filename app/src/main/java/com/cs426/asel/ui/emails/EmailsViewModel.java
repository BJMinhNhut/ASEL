package com.cs426.asel.ui.emails;

import static java.lang.Math.min;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.ViewModel;
import com.cs426.asel.backend.GmailServices;
import com.cs426.asel.backend.Mail;
import com.cs426.asel.backend.MailList;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.api.services.gmail.model.Message;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EmailsViewModel extends ViewModel implements GmailServices.EmailCallback {
    private static final int MAX_RETRY_COUNT = 5;
    private static final int RETRY_DELAY_MS = 1000;
    private static final int EMAIL_PER_FETCH = 20;

    private GmailServices gmailServices;
    private Map<String, Message> messageCache;
    private Map<ListenableFuture<GenerateContentResponse>, Integer> retryCounts;

    private ListeningExecutorService executorService;
    private ScheduledExecutorService scheduledExecutor;

    private MailList mailList;
    private Context context;

    public EmailsViewModel(Context context) {
        this.context = context;
        messageCache = new HashMap<>();
        gmailServices = new GmailServices(context, this); // Initialize GmailServices for email operations
        mailList = new MailList();
    }

    public void fetchEmails() {
        fetchAllEmailsID(); // Step 1: Fetch all email IDs
    }

    public void fetchAllEmailsID() {
        gmailServices.fetchAllEmailIDs();
    }

    private List<String> getUnfetchedID() {
        List<String> unfetchedID = new ArrayList<>();
        for (Map.Entry<String, Message> entry: messageCache.entrySet()) {
            if (entry.getValue() == null) {
                unfetchedID.add(entry.getKey());
            }
        }
        return unfetchedID;
    }

    private void fetchAllEmailsContent() {
        ExecutorService executor = Executors.newFixedThreadPool(4); // Adjust the pool size as needed
        List<Callable<Void>> tasks = new ArrayList<>();


        List<String> unfetchedID = getUnfetchedID();
        fetchEmailContent(unfetchedID);
    }

    public void fetchEmailContent(List<String>id) {
        gmailServices.fetchEmailByIds(id, new GmailServices.FetchEmailCallback() {
            @Override
            public void onEmailFetched(List<Message> messages) {
                for (Message message: messages) {
                    storeMessage(message.getId(), message);
                    Log.d("EmailsViewModel", "Fetched " + message.getId() + ": " + message.getSnippet());
                }
                processEmails();
            }

            @Override
            public void onFetchEmailFailed(String errorMessage) {
                Log.e("EmailsViewModel", errorMessage);
            }
        });
    }

    @Override
    public void onEmailIDsFetched(List<Message> emailIDs) {
        if (emailIDs != null) {
            reset();
            for (Message message: emailIDs) {
                String id = message.getId();
                storeID(message.getId());
            }
            fetchAllEmailsContent(); // Step 2: Fetch all emails content after IDs are fetched
        }
    }

    @Override
    public void onEmailContentFetched() {
        // Process emails or perform other actions after all content is fetched
        Log.d("EmailsViewModel", "All email content fetched.");
        //processEmails();
    }

    private void processEmails() {
        int processSize = min(EMAIL_PER_FETCH, messageCache.size());
        Log.d("EmailsViewModel", "Processing " + processSize + " emails.");
        List<ListenableFuture<GenerateContentResponse>> futures = new ArrayList<>();
        retryCounts = new HashMap<>();
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(processSize));
        scheduledExecutor = Executors.newScheduledThreadPool(1);

        for (int i = 0; i < processSize; i++) {
            Map.Entry<String, Message> entry = messageCache.entrySet().iterator().next();
            if (entry.getValue() == null) {
                Log.d("EmailsViewModel", "Email ID " + entry.getKey() + " is null. Skipping.");
                continue;
            }

            Mail mail = new Mail(entry.getValue());
            ListenableFuture<GenerateContentResponse> future = mail.summarize();

            mailList.addMail(mail);
            retryCounts.put(future, 0);
            futures.add(future);
            attemptProcessWithRetry(mail, future, 0);
        }

        ListenableFuture<List<GenerateContentResponse>> allFutures = Futures.allAsList(futures);
        Futures.addCallback(allFutures,
                new FutureCallback<List<GenerateContentResponse>>() {
                    @Override
                    public void onSuccess(List<GenerateContentResponse> result) {
                        Log.d("EmailsViewModel", "All emails processed successfully.");
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.d("EmailsViewModel", "Error processing all emails.");
                        int count = 0;
                        for (Map.Entry<ListenableFuture<GenerateContentResponse>, Integer> entry : retryCounts.entrySet()) {
                            if (entry.getValue() >= MAX_RETRY_COUNT) {
                                count++;
                            }
                        }

                        Log.d("EmailsViewModel", "Failed to process " + count + " emails.");
                    }
                }, executorService);
    }

    private void attemptProcessWithRetry(Mail mail,
                                         ListenableFuture<GenerateContentResponse> future,
                                         int retryCount) {
        Futures.addCallback(
                future,
                new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        mail.setSummary(result.getText());
                        Log.d("EmailsViewModel", mail.getSummary());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e("EmailsViewModel", "Error processing email ID " + mail.getId(), t);
                        if (retryCount < MAX_RETRY_COUNT) {
                            int newRetryCount = retryCount + 1;
                            retryCounts.put(future, newRetryCount);
                            scheduleRetry(mail, newRetryCount);
                        } else {
                            Log.e("EmailsViewModel", "Max retry count reached for email ID " + mail.getId());
                        }
                    }
                },
                executorService
        );
    }

    private void scheduleRetry(Mail mail, int newRetryCount) {
        scheduledExecutor.schedule(() -> {
            ListenableFuture<GenerateContentResponse> newFuture = mail.summarize();
            retryCounts.put(newFuture, newRetryCount);
            attemptProcessWithRetry(mail, newFuture, newRetryCount);
        }, RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    public Message getMessage(String id) {
        if (!messageCache.containsKey(id)) {
            return null;
        }
        return messageCache.getOrDefault(id, null);
    }

    public ArrayList<Message> getMessages() {
        return new ArrayList<>(messageCache.values());
    }

    public void reset() {
        messageCache.clear();
    }

    public void storeID(String id) {
        if (!messageCache.containsKey(id)) {
            messageCache.put(id, null);
        }
    }

    public void storeMessage(String id, Message message) {
        if (messageCache.containsKey(id)) {
            messageCache.put(id, message);
        }
        else {
            Log.d("EmailsViewModel", "Found no id " + id + " in message cache");
        }
    }
}
