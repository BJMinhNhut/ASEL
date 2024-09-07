package com.cs426.asel.backend;

import android.content.Context;

import com.google.api.services.gmail.model.Message;

import java.util.ArrayList;
import java.util.List;

public class MailList {
    private List<Mail> mailList;

    public MailList() {
        mailList = new ArrayList<>();
    }

    public void updateEmails(Context context, List<Message> messages) {
        // Fetch Messages from Gmail into a List and add to database


        // Fet  ch existing Mail from Database to fill up displaying list

    }

    public Mail getMail(int index) {
        return mailList.get(index);
    }

    public Mail findMailbyID(String id) {
        for (Mail mail : mailList) {
            if (mail.getEmailID().equals(id)) {
                return mail;
            }
        }
        return null;
    }

    public void addMail(Mail mail) {
        mailList.add(mail);
    }

    public void removeMail(int index) {
        mailList.remove(index);
    }

    public int size() {
        return mailList.size();
    }
}
