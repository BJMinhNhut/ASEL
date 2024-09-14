package com.cs426.asel.ui.emails;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cs426.asel.MainActivity;
import com.cs426.asel.backend.GmailServices;
import com.cs426.asel.backend.GoogleAccountServices;
import com.cs426.asel.backend.Mail;
import com.cs426.asel.backend.MailList;
import com.cs426.asel.backend.MailRepository;
import com.cs426.asel.backend.Utility;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
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
    private static final int EMAIL_PER_FETCH = 10; // for fetching content
    private static final int ID_PER_FETCH = 200; // for fetching IDs
    private int currentIndex = 0;

    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);

    private final GmailServices gmailServices;
    private Map<String, Message> messageCache;
    private Map<ListenableFuture<GenerateContentResponse>, Integer> retryCounts;

    private ListeningExecutorService executorService;
    private ScheduledExecutorService scheduledExecutor;
    private Context context;

    public EmailsViewModel(Context context) {
        this.context = context;
        gmailServices = new GmailServices(context, this); // Initialize GmailServices for email operations
//        processedIDs = new ArrayList<>();
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void fetchAllEmailsID() {
        gmailServices.fetchAllEmailIDs();
    }

    public void fetchNextIdBatch() {
        gmailServices.fetchNextIdBatch(ID_PER_FETCH);
    }

    private List<String> getUnfetchedID() {
        List<String> unfetchedID = new ArrayList<>();
        String userEmail = Utility.getUserEmail(context);
        MailRepository mailRepository = new MailRepository(context, userEmail);

        for (Map.Entry<String, Message> entry: messageCache.entrySet()) {
            if (!mailRepository.isMailExists(entry.getKey())) {
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
                }
                onEmailContentFetched();
            }

            @Override
            public void onFetchEmailFailed(String errorMessage) {
                Log.e("EmailsViewModel", errorMessage);
            }
        });
    }

    public void setUserEmailAddress() {

    }

    @Override
    public void onEmailIDsFetched(List<Message> emailIDs) {
        if (emailIDs != null) {
            reset();
            for (Message message: emailIDs) {
                storeID(message.getId());
            }

            // Step 2: if no new mail then fetch id again, otherwise fetch all emails content after IDs are fetched
            List<String> unfetchedID = getUnfetchedID();
            if (unfetchedID.isEmpty()) {
                Log.w("EmailsViewModel", "No new emails found. Fetching more IDs.");
                isLoading.postValue(false);
                loadMoreEmails();
            } else {
                Log.d("EmailsViewModel", "Found " + unfetchedID.size() + " unfetched emails.");
                fetchEmailContent(unfetchedID);
            }
        }
        Log.d("EmailsViewModel", "Message cache size:" + messageCache.size());
    }

    @Override
    public void onEmailContentFetched() {
        // Process emails or perform other actions after all content is fetched
        Log.d("EmailsViewModel", "All email content fetched.");
        processEmails();
    }

    public void loadMoreEmails() {
        if (Boolean.TRUE.equals(isLoading.getValue())) {
            return;
        }
        isLoading.postValue(true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            if (needMoreIds()) {
                Log.d("EmailsViewModel", "All emails are processed. Try to fetch more");
                fetchNextIdBatch();
            } else processEmails();
        });
    }

    private boolean needMoreIds() {
        return currentIndex + ID_PER_FETCH > messageCache.size();
    }

    private void processEmails() {
        int processLimit = min(currentIndex + EMAIL_PER_FETCH, messageCache.size());
        int processSize = processLimit - currentIndex;
        String userEmail = Utility.getUserEmail(context);
        MailRepository mailRepository = new MailRepository(context, userEmail);

        List<ListenableFuture<GenerateContentResponse>> futures = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(processSize);
        Log.d("EmailsViewModel", "Processing emails from index " + currentIndex + " to " + processLimit);

        retryCounts = new HashMap<>();
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(processSize));
        scheduledExecutor = Executors.newScheduledThreadPool(1);

        List<String> ids = getEmailsID();

        for (int i = currentIndex; i < processLimit; i++) {
            String curId = ids.get(i);
            if (isProcessed(curId)) {
                Log.d("EmailsViewModel", "Email ID " + curId + " is already processed. Skipping.");
                Mail mail = mailRepository.getMailById(curId);
                latch.countDown();
                continue;
            }

            Message message = getMessage(curId);
            if (message == null) {
                Log.d("EmailsViewModel", "Email ID " + curId + " is null. Skipping.");
                continue;
            }

            Mail mail = new Mail(message);
            ListenableFuture<GenerateContentResponse> future = mail.summarize();

            retryCounts.put(future, 0);
            futures.add(future);
            attemptProcessWithRetry(mail, future, latch);
        }

        currentIndex = processLimit;

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
                        mail.extractInfo(result.getText());

                        String userEmail = Utility.getUserEmail(context);
                        MailRepository mailRepository = new MailRepository(context, userEmail);
                        mailRepository.insertMail(mail);
                        Log.d("EmailsViewModel", "Email ID: " + mail.getId() + ", is in db?: " + mailRepository.isMailExists(mail.getId()));
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e("EmailsViewModel", "Error processing email ID " + mail.getId(), t);
                        int retryCount = retryCounts.getOrDefault(future, 0);
                        if (retryCount < MAX_RETRY_COUNT) {
                            int newRetryCount = retryCount + 1;
                            retryCounts.put(future, newRetryCount);
                            scheduleRetry(mail, newRetryCount, latch);
                        } else {
                            Log.e("EmailsViewModel", "Max retry count reached for email ID " + mail.getId());
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
        if (!messageCache.containsKey(id)) {
            return null;
        }
        return messageCache.getOrDefault(id, null);
    }

    public ArrayList<Message> getMessages() {
        return new ArrayList<>(messageCache.values());
    }

    public List<String> getEmailsID() {
        return new ArrayList<>(messageCache.keySet());
    }

    public void reset() {
        if (messageCache != null) {
            messageCache.clear();
            currentIndex = 0;
        } else {
            messageCache = new LinkedHashMap<>();
        }
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

    private boolean isProcessed(String id) {
        String userEmail = Utility.getUserEmail(context);
        MailRepository mailRepository = new MailRepository(context, userEmail);
        return mailRepository.isMailExists(id);
    }

    public MailList getMailList() {
        String userEmail = Utility.getUserEmail(context);
        MailRepository mailRepository = new MailRepository(context, userEmail);
        return mailRepository.getMailByRead(false, "send_time", false);
    }

    public MailList getMailListFrom(int index) {
        String userEmail = Utility.getUserEmail(context);
        MailRepository mailRepository = new MailRepository(context, userEmail);
        MailList list = mailRepository.getMailByRead(false, "send_time", false);
        MailList res = new MailList();

        for (int i = index; i < min(list.size(), index + EMAIL_PER_FETCH); i++) {
            Mail mail = list.getMail(i);
            Log.d("EmailsViewModel", "Adding mail " + mail.getId() + " to mail list");
            res.addMail(list.getMail(i));
        }
        return res;
    }
}
