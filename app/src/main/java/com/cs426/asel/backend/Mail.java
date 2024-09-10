package com.cs426.asel.backend;

import android.util.Log;
import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.api.client.util.StringUtils;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.codec.binary.Base64;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.w3c.dom.Text;

import java.time.Instant;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.security.auth.callback.Callback;

public class Mail {
    private final String mId;
    private String mTitle;
    private String mSender;
    private String mReceiver;
    private String mContent;
    private String mSummary;
    private String mSentTime;
    private String mTag;
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
        mSentTime = "";
        mReceivedTime = Instant.now();
    }

    public Mail(String id, String title, String sender, String receiver, String content, String summary, Event event, Instant receivedTime, boolean isRead, String tag) {
        mId = id;
        mTitle = title;
        mSender = sender;
        mReceiver = receiver;
        mContent = content;
        mSummary = summary;
        mEvent = event;
        mReceivedTime = receivedTime;
        mIsRead = isRead;
        mTag = tag;
    }

    public Mail(Message message) {
        mTitle = "";
        mSender = "";
        mReceiver = "";
        mContent = "";
        mSummary = "";
        mEvent = new Event();
        mIsRead = false;
        mSentTime = "";
        mReceivedTime = Instant.now();

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
            } else if (h.getName().equals("Date")) {
                mSentTime = h.getValue();
            }
        }

        mContent = getDecodedBody(message);

        if (mId.equals("1916d9c8aa7f530d")) {
            Log.d("Mail", "Retarded mail content: " + mContent) ;
        }
    }

    private static String getDecodedBody(Message message) {
        String body = "";

        List<MessagePart> parts = message.getPayload().getParts();
        if (parts == null) {
            body = htmlToPlainText(StringUtils.newStringUtf8(Base64.decodeBase64(message.getPayload().getBody().getData())));
            Log.d("Mail", "Snippet: " + message.getSnippet());
            Log.d("Mail", "Body: " + body);
            return body;
        }
        for (MessagePart part : parts) {
            try {
                body += extractTextFromMimeMessage(part);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return body;
    }

    public static String extractTextFromMimeMessage(MessagePart part) throws Exception {
        if (part.getMimeType().equals("text/plain")) {
            return StringUtils.newStringUtf8(Base64.decodeBase64(part.getBody().getData()));
        }

        if (part.getMimeType().equals("text/html")) {
            String html = StringUtils.newStringUtf8(Base64.decodeBase64(part.getBody().getData()));
            return htmlToPlainText(html);
        }

        if (part.getMimeType().startsWith("multipart/")) {
            List<MessagePart> parts = part.getParts();
            for (MessagePart childPart : parts) {
                String text = extractTextFromMimeMessage(childPart);
                if (text != null) {
                    return text;
                }

                String result = extractTextFromMimeMessage(childPart);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static String htmlToPlainText(String html) {
        Document doc = Jsoup.parse(html);
        return doc.text();
    }

    public ListenableFuture<GenerateContentResponse> summarize() {
        String prompt = "Please provide a brief summary of the email below  with this schema: { \"summary\": str, \"fromDateTime\": str, \"toDateTime\": str, \"location\": str, \"tag\": str}. Provide a short and general summary of the content. If there is an event, provide the fromDateTime, toDateTime, and location of the event, else put null. If there is only one time mark or deadline, put it in fromDateTime and leave toDateTime null. The DateTime should be given in the format \"DD/MM/YYYY/, hh:mm\", if there is date but no specific hour or minute, put 00:00 for hh:mm. Provide the tag of mail (Assignment, Exam, Meeting, Course Material, Other):";

        return ChatGPTUtils.getResponse(prompt + mContent);
    }

    public void extractInfo(String content) {
        Log.d("Mail", "Extracting info from content: " + content);
        MailInfo mailInfo;
        try {
            mailInfo = new ObjectMapper().readValue(content, MailInfo.class);
        } catch (Exception e) {
            Log.e("Mail", "Error parsing JSON");
            e.printStackTrace();
            mailInfo = new MailInfo();
        }

        mSummary = mailInfo.summary;

        int duration = 0;
        if (mailInfo.toDateTime != null) {
            duration = (int) java.time.Duration.between(mailInfo.fromDateTime, mailInfo.toDateTime).toMinutes();
        }

        if (mailInfo.fromDateTime != null) {
            mEvent = new Event(
                    mId,
                    mTitle,
                    mailInfo.fromDateTime,
                    duration,
                    mailInfo.location,
                    false,
                    "None",
                    mailInfo.toDateTime,
                    mailInfo.fromDateTime.minusSeconds(60 * 5),
                    mSummary,
                    false
            );
        } else {
            mEvent = new Event();
        }
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

    public String getSentTime() {
        return mSentTime;
    }

    public boolean isRead() {
        return mIsRead;
    }

    public void setRead(boolean isRead) {
        mIsRead = isRead;
    }

    public void setSummary(String summary) {
        mSummary = summary;
    }

    public void setEvent(Event event) {
        mEvent = event;
    }
    public String getLocation() {
        return mEvent.getLocation();
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MailInfo {
        private String location = "nothing to show";
        private Instant fromDateTime;
        private Instant toDateTime;
        private String toTime = "nothing to show";
        private String summary = "nothing to show";
        private String tag = "nothing to show";

        @JsonProperty("fromDateTime")
        public void unpackFromDateTime(String fromDateTime) {
            if (fromDateTime == null || fromDateTime.equals("null")) {
                this.fromDateTime = null;
                return;
            }

            this.fromDateTime = LocalDateTime.parse(
                    fromDateTime,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm")
            ).atZone(
                    ZoneId.of("Asia/Ho_Chi_Minh")
            ).toInstant();
        }

        @JsonProperty("toDateTime")
        public void unpackToDateTime(String toDateTime) {
            if (toDateTime == null || toDateTime.equals("null")) {
                this.toDateTime = this.fromDateTime;
                return;
            }

            this.toDateTime = LocalDateTime.parse(
                    toDateTime,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm")
            ).atZone(
                    ZoneId.of("Asia/Ho_Chi_Minh")
            ).toInstant();
        }
    }
}
