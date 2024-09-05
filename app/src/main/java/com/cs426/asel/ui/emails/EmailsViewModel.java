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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailsViewModel extends ViewModel implements GmailServices.EmailCallback {
    private GmailServices gmailServices;
    private Map<String, Message> messageCache;
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

    private void fetchAllEmailsContent() {
        ExecutorService executor = Executors.newFixedThreadPool(4); // Adjust the pool size as needed
        List<Callable<Void>> tasks = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(messageCache.size());

        for (String id : messageCache.keySet()) {
            tasks.add(() -> {
                fetchEmailContent(id, latch);
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
        if (messageCache.containsKey(id) && messageCache.get(id) != null) {
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
        Log.d("EmailsViewModel", "Processing " + messageCache.size() + " emails.");
        Executor executor = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(messageCache.size());

        for (Map.Entry<String, Message> entry : messageCache.entrySet()) {
            if (entry.getValue() == null) {
                Log.d("EmailsViewModel", "Email ID " + entry.getKey() + " is null. Skipping.");
                continue;
            }
            Mail mail = new Mail(entry.getValue());
            mailList.addMail(mail);

            ListenableFuture<GenerateContentResponse> future = mail.summarize();
            Futures.addCallback(
                    future,
                    new FutureCallback<GenerateContentResponse>() {
                        @Override
                        public void onSuccess(GenerateContentResponse result) {
                            latch.countDown();
                            mail.setSummary(result.getText());
                            Log.d("EmailsViewModel", mail.getSummary());
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            latch.countDown();
                            Log.e("EmailsViewModel", "Error processing email ID " + entry.getKey(), t);
                        }
                    },
                    executor
            );
        }

        try {
            latch.await();
            Log.d("EmailsViewModel", "All emails processed.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Message getMessage(String id) {
        return messageCache.getOrDefault(id, null);
    }

    public ArrayList<Message> getMessages() {
        ArrayList<Message> messages = new ArrayList<>();
        for (Map.Entry<String, Message> entry : messageCache.entrySet()) {
            if (entry.getValue() == null) {
                Log.d("EmailsViewModel", "Email ID " + entry.getKey() + " is null. Skipping.");
                continue;
            }
            messages.add(entry.getValue());
        }
        return messages;
    }

    public List<String> getEmailsID() {
        return new ArrayList<>(messageCache.keySet());
    }

    public void reset() {
        messageCache.clear();
    }

    public void storeID(String id) {
        if (!messageCache.containsKey(id)) {
            messageCache.put(id, null); // Placeholder for future message content
        }
    }

    public void storeMessage(String id, Message message) {
        if (message != null) {
            messageCache.put(id, message);
        }
    }
}
