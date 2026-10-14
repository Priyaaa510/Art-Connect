package com.example.ArtConnect.notification;

/** Sends invoice emails. Chosen with app.notifications.email.provider: "outbox" (default demo) or "smtp". */
public interface EmailSender {
    /** Short name stored with each notification, e.g. "smtp". */
    String provider();

    /** True when nothing is really delivered (the demo outbox). */
    boolean simulated();

    void send(String to, String subject, String html, String plainText) throws Exception;
}
