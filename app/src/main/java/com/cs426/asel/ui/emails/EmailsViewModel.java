package com.cs426.asel.ui.emails;

import static java.lang.Math.min;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EmailsViewModel extends ViewModel implements GmailServices.EmailCallback {
    private static final int MAX_RETRY_COUNT = 5;
    private static final int RETRY_DELAY_MS = 1000;
    private static final int EMAIL_PER_FETCH = 15;

    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);

    private GmailServices gmailServices;
    private List<String> idList;
    private List<String> processedIDs;
    private Map<String, Message> messageCache;
    private Map<ListenableFuture<GenerateContentResponse>, Integer> retryCounts;

    private ListeningExecutorService executorService;
    private ScheduledExecutorService scheduledExecutor;

    private MailList mailList;
    private Context context;
    private EmailsFragment fragment;

    public EmailsViewModel(Context context) {
        this.context = context;
        idList = new ArrayList<>();
        gmailServices = new GmailServices(context, this); // Initialize GmailServices for email operations
        mailList = new MailList();
        processedIDs = new ArrayList<>();
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void fetchEmails() {
        fetchAllEmailsID(); // Step 1: Fetch all email IDs
    }

    public void fetchAllEmailsID() {
        gmailServices.fetchAllEmailIDs();
    }

    private void fetchAllEmailsContent() {
        ExecutorService executor = Executors.newFixedThreadPool(4); // Adjust the pool size as needed
        List<Callable<Void>> tasks = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(min(EMAIL_PER_FETCH, idList.size()));

        for (int i = 0; i < min(EMAIL_PER_FETCH, idList.size()); i++) {
            if (processedIDs.contains(idList.get(i))) {
                continue;
            }
            final int index = i;
            tasks.add(() -> {
                fetchEmailContent(idList.get(index), latch);
                return null;
            });
        }

        try {
            executor.invokeAll(tasks); // This will block until all tasks are completed
            latch.await(); // Wait until all tasks signal completion
            onEmailContentFetched(); // This will be called once all emails are fetched
        } catch (InterruptedException e) {
            e.printStackTrace(); // Handle exception as needed
        } finally {
            executor.shutdown(); // Shut down the executor after all tasks are complete
        }
    }

    public void fetchEmailContent(String id, CountDownLatch latch) {
        if (processedIDs.contains(id)) {
            latch.countDown(); // Decrement latch count even if the message is already fetched
            return; // Return cached message if already fetched
        } else {
            gmailServices.fetchEmailById(id, new GmailServices.FetchEmailCallback() {
                @Override
                public void onEmailFetched(Message message) {
                    storeMessage(id, message);
                    Log.d("EmailsViewModel", "Email ID " + id + " fetched and stored.");
                    latch.countDown(); // Signal completion of this task
                }

                @Override
                public void onFetchEmailFailed(String errorMessage) {
                    Log.e("EmailsViewModel", errorMessage);
                    latch.countDown(); // Signal completion even if the task failed
                }
            });
        }
    }

    @Override
    public void onEmailIDsFetched(List<String> emailIDs) {
        if (emailIDs != null) {
            reset();
            for (String id : emailIDs) {
                storeID(id);
            }

            fetchAllEmailsContent(); // Step 2: Fetch all emails content after IDs are fetched
        }
    }

    @Override
    public void onEmailContentFetched() {
        // Process emails or perform other actions after all content is fetched
        Log.d("EmailsViewModel", "All email content fetched.");

        processEmails();
    }

    private void processEmails() {
        int processSize = min(EMAIL_PER_FETCH, idList.size());
        Log.d("EmailsViewModel", "Processing " + processSize + " emails.");
        List<ListenableFuture<GenerateContentResponse>> futures = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(processSize);

        retryCounts = new HashMap<>();
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(processSize));
        scheduledExecutor = Executors.newScheduledThreadPool(1);

        for (int i = 0; i < processSize; i++) {
            String curId = idList.get(i);
            if (isProcessed(curId)) {
                // TODO: fetch from db, add to mail list
                latch.countDown();
                continue;
            }

            processedIDs.add(idList.get(i));
            Message message = getMessage(curId);
            if (message == null) {
                Log.d("EmailsViewModel", "Email ID " + curId + " is null. Skipping.");
                continue;
            }

            Mail mail = new Mail(message);
            ListenableFuture<GenerateContentResponse> future = mail.summarize();

            mailList.addMail(mail);
            retryCounts.put(future, 0);
            futures.add(future);
            attemptProcessWithRetry(mail, future, latch);

            processedIDs.add(curId);
        }

        try {
            latch.await();
            Log.d("EmailsViewModel", "All emails processed.");
            executorService.shutdown();
            scheduledExecutor.shutdown();
            isLoading.postValue(false);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void attemptProcessWithRetry(Mail mail,
                                         ListenableFuture<GenerateContentResponse> future,
                                         CountDownLatch latch) {
        Futures.addCallback(
                future,
                new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        latch.countDown();
                        Log.d("EmailsViewModel", "Email ID " + mail.getEmailID() + " processed.");
                        mail.extractInfo(result.getText());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e("EmailsViewModel", "Error processing email ID " + mail.getEmailID(), t);
                        int retryCount = retryCounts.getOrDefault(future, 0);
                        if (retryCount < MAX_RETRY_COUNT) {
                            int newRetryCount = retryCount + 1;
                            retryCounts.put(future, newRetryCount);
                            scheduleRetry(mail, newRetryCount, latch);
                        } else {
                            Log.e("EmailsViewModel", "Max retry count reached for email ID " + mail.getEmailID());
                            latch.countDown();
                        }
                    }
                },
                executorService
        );
    }

    private void scheduleRetry(Mail mail, int newRetryCount, CountDownLatch latch) {
        scheduledExecutor.schedule(() -> {
            ListenableFuture<GenerateContentResponse> newFuture = mail.summarize();
            retryCounts.put(newFuture, newRetryCount);
            attemptProcessWithRetry(mail, newFuture, latch);
        }, RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    public Message getMessage(String id) {
        return messageCache.getOrDefault(id, null);
    }


    public List<String> getEmailsID() {
        return new ArrayList<>(messageCache.keySet());
    }

    public void reset() {
        if (messageCache != null) {
            messageCache.clear();
        } else {
            messageCache = new LinkedHashMap<>();
        }
    }

    public void storeID(String id) {
        if (!idList.contains(id)) {
            idList.add(id); // Placeholder for future message content
        }
    }

    public void storeMessage(String id, Message message) {
        if (message != null) {
            messageCache.put(id, message);
        }
    }

    private boolean isProcessed(String id) {
        return processedIDs.contains(id);
    }

    public MailList getMailList() {
        return mailList;
    }
}
