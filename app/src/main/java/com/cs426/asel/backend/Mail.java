package com.cs426.asel.backend;

import android.content.Context;

import com.google.api.client.util.StringUtils;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Text;

import java.util.List;

public class Mail {
    private final String emailID;
    private String title;
    private String sender;
    private String content;
    private String summary;

    public Mail(Message message) {
        title = "";
        sender = "";
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
            }
        }

        if (parts != null) {
            for (MessagePart part : parts) {
                if (part.getMimeType().equals("text/plain")) {
                    content = new String(Base64.decodeBase64(part.getBody().getData()));
                    break;
                }
            }
        } else {
            content = new String(Base64.decodeBase64(message.getPayload().getBody().getData()));
        }
    }

    public void summarize(Context context) {
        // Summarize content using ChatGPT
        summary = ChatGPTUtils.getMailSummary(this, context);
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

    public String getSummary() {
        return summary;
    }

    public String getContent() {
        return content;
    }
}
