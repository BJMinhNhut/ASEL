package com.cs426.asel.backend;

import android.content.Context;

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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.security.auth.callback.Callback;

public class Mail {
    private final String emailID;
    private String title;
    private String sender;
    private String receiver;
    private String content;
    private String summary;

    public Mail(Message message) {
        title = "";
        sender = "";
        receiver = "";
        content = "";
        summary = "";

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
            }
        }

        content = getDecodedBody(message);
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
        return ChatGPTUtils.getResponse(prompt + content);
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
        return summary;
    }

    public String getContent() {
        return content;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public class MailInfo {
        private String location;
        private String fromDate;
        private String toDate;
        private String fromTime;
        private String toTime;
        private String summary;
    }
}
