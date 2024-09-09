package com.cs426.asel.backend;

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

import java.io.ByteArrayInputStream;
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
    private final String emailID;
    private boolean isRead;
    private String title;
    private String sender;
    private String receiver;
    private String content;
    private String summary;
    private String sendTime;

    private MailInfo mailInfo;

    public Mail(Message message) {
        title = "";
        sender = "";
        receiver = "";
        content = "";
        summary = "";
        isRead = false;

        emailID = message.getId();
        List<MessagePartHeader> header = message.getPayload().getHeaders();
        List<MessagePart> parts = message.getPayload().getParts();

        for (MessagePartHeader h : header) {
            if (h.getName().equals("Subject")) {
                title = h.getValue();
            } else if (h.getName().equals("From")) {
                sender = h.getValue();
            } else if (h.getName().equals("To")) {
                receiver = h.getValue();
            } else if (h.getName().equals("Date")) {
                sendTime = h.getValue();
            }
        }

        content = getDecodedBody(message);
    }

    private String getDecodedBody(Message message) {
        String body = "";

        List<MessagePart> parts = message.getPayload().getParts();
        if (parts == null) {
            Log.d("Mail", "Snippet: " + message.getSnippet());
            return message.getSnippet();
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
        String prompt = "Provide a brief summary of the email below. The summary should be short and general. If there is an event, provide the fromDateTime and toDateTime, location, else put null. If there is only one time mark or deadline, put it in fromDateTime and leave toDateTime null. The DateTime should be given in the format \"DD/MM/YYYY/ # hh:mm\". Provide the tag of mail (Assignment, Exam, Meeting, Course Material, Other):";
        return ChatGPTUtils.getResponse(prompt + content);
    }

    public void extractInfo(String content) {
        Log.d("Mail", "Extracting info from content: " + content);
        try {
            mailInfo = new ObjectMapper().readValue(content, MailInfo.class);
        } catch (Exception e) {
            Log.e("Mail", "Error parsing JSON");
            e.printStackTrace();
            mailInfo = new MailInfo();
        }
    }

    public String getEmailID() {
        return emailID;
    }

    public String getTitle() {
        return title;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getSummary() {
        return mailInfo.summary;
    }

    public String getContent() {
        return content;
    }

    public String getSendTime() {
        return sendTime;
    }

    public String getLocation() {
        return mailInfo.location;
    }

    public String getFromDate() {
        return mailInfo.fromDate;
    }

    public String getToDate() {
        return mailInfo.toDate;
    }

    public String getFromTime() {
        return mailInfo.fromTime;
    }

    public String getToTime() {
        return mailInfo.toTime;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MailInfo {
        private String location = "nothing to show";
        private String fromDate = "nothing to show";
        private String toDate = "nothing to show";
        private String fromTime = "nothing to show";
        private String toTime = "nothing to show";
        private String summary = "nothing to show";
        private String tag = "nothing to show" ;

        @SuppressWarnings("unchecked")
        @JsonProperty("fromDateTime")
        public void unpackFromDateTime(String fromDateTime) {
            if (fromDateTime == null) {
                fromDate = "none";
                fromTime = "none";
                return;
            }
            // Separate string by '#'
            String[] parts = fromDateTime.split(" # ");
            fromDate = parts[0];
            fromTime = parts[1];
        }

        @JsonProperty("toDateTime")
        public void unpackToDateTime(String toDateTime) {
            if (toDateTime == null) {
                toDate = "none";
                toTime = "none";
                return;
            }

            String[] parts = toDateTime.split(" # ");
            toDate = parts[0];
            toTime = parts[1];
        }
    }
}
