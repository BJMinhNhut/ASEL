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

    public Mail getMail(int index) {
        return mailList.get(index);
    }

    public Mail findMailbyID(String id) {
        for (Mail mail : mailList) {
            if (mail.getId().equals(id)) {
                return mail;
            }
        }
        return null;
    }

    public void append(MailList mailList) {
        this.mailList.addAll(mailList.mailList);
    }

    public void addMail(Mail mail) {
        mailList.add(mail);
    }

    public void insertMailAt(Mail mail, int index) {
        mailList.add(index, mail);
    }

    public void removeMail(int index) {
        mailList.remove(index);
    }

    public int size() {
        return mailList.size();
    }
}
