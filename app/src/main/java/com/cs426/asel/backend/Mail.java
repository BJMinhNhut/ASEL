package com.cs426.asel.backend;

import android.util.Log;

import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.codec.binary.Base64;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.time.Instant;
import java.util.List;

public class Mail {
    private final String mId;
    private String mTitle;
    private String mSender;
    private String mReceiver;
    private String mContent;
    private String mSummary;
    private Event mEvent;
    private Instant mReceivedTime;
    private boolean mIsRead;

    public Mail() {
        mId = "";
        mTitle = "";
        mSender = "";
        mReceiver = "";
        mContent = "";
        mSummary = "";
        mEvent = new Event();
        mIsRead = false;
        mReceivedTime = Instant.now();
    }

    public Mail(String id, String title, String sender, String receiver, String content, String summary, Event event, Instant receivedTime, boolean isRead) {
        mId = id;
        mTitle = title;
        mSender = sender;
        mReceiver = receiver;
        mContent = content;
        mSummary = summary;
        mEvent = event;
        mReceivedTime = receivedTime;
        mIsRead = isRead;
    }

    public Mail(Message message) {
        mTitle = "";
        mSender = "";
        mReceiver = "";
        mContent = "";
        mSummary = "";

        mId = message.getId();
        List<MessagePartHeader> header = message.getPayload().getHeaders();
        List<MessagePart> parts = message.getPayload().getParts();

        for (MessagePartHeader h : header) {
            if (h.getName().equals("Subject")) {
                mTitle = h.getValue();
            } else if (h.getName().equals("From")) {
                mSender = h.getValue();
            } else if (h.getName().equals("To")) {
                mReceiver = h.getValue();
            }
        }

        mContent = getDecodedBody(message);
    }

    private String getDecodedBody(Message message) {
        String body = "";
        if (message.getPayload() != null) {
            List<MessagePart> parts = message.getPayload().getParts();
            if (parts != null && !parts.isEmpty()) {
                for (MessagePart part : parts) {
                    if (part.getMimeType().equals("text/plain")) {
                        body = new String(Base64.decodeBase64(part.getBody().getData()));
                    } else if (part.getMimeType().equals("text/html")) {
                        String htmlBody = new String(Base64.decodeBase64(part.getBody().getData()));
                        body = htmlToPlainText(htmlBody); // Convert HTML to plain text
                    }
                }
            } else {
                String mimeType = message.getPayload().getMimeType();
                String data = message.getPayload().getBody().getData();
                if ("text/plain".equals(mimeType)) {
                    body = new String(Base64.decodeBase64(data));
                } else if ("text/html".equals(mimeType)) {
                    String htmlBody = new String(Base64.decodeBase64(data));
                    body = htmlToPlainText(htmlBody); // Convert HTML to plain text
                }
            }
        }
        return body;
    }

    private String htmlToPlainText(String html) {
        Document doc = Jsoup.parse(html);
        return doc.text();
    }

    public ListenableFuture<GenerateContentResponse> summarize() {
        String prompt = "Give a brief and general summary of the following email content in a short paragraph";
        Log.d("Mail", "Summarizing email with ID: " + mId + " and content: " + mContent);
        return ChatGPTUtils.getResponse(prompt + mContent);
    }

    public String getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getSender() {
        return mSender;
    }

    public String getReceiver() {
        return mReceiver;
    }

    public String getSummary() {
        return mSummary;
    }

    public String getContent() {
        return mContent;
    }

    public Event getEvent() {
        return mEvent;
    }

    public Instant getReceivedTime() {
        return mReceivedTime;
    }

    public boolean isRead() {
        return mIsRead;
    }

    public void setRead(boolean isRead) {
        mIsRead = isRead;
    }

    public void setSummary(String mSummary) {
        mSummary = mSummary;
    }

    public void setEvent(Event event) {
        mEvent = event;
    }

    public void setContent(String content) {
        mContent = content;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setSender(String sender) {
        mSender = sender;
    }

    public void setReceiver(String receiver) {
        mReceiver = receiver;
    }

    public void setReceivedTime(Instant receivedTime) {
        mReceivedTime = receivedTime;
    }
}
