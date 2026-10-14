package com.example.ArtConnect.notification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Demo "delivery" for local development: instead of sending, invoices are written to the outbox folder
 * (emails as .html files you can open in a browser) and logged.
 */
public final class OutboxSenders {
    private static final Logger log = LoggerFactory.getLogger(OutboxSenders.class);
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private OutboxSenders() {}

    @Component
    @ConditionalOnProperty(name = "app.notifications.email.provider", havingValue = "outbox", matchIfMissing = true)
    public static class OutboxEmailSender implements EmailSender {
        private final Path outbox;

        public OutboxEmailSender(@Value("${app.notifications.outbox-dir:outbox}") String outboxDir) {
            this.outbox = Path.of(outboxDir).toAbsolutePath();
        }

        @Override public String provider() { return "outbox"; }
        @Override public boolean simulated() { return true; }

        @Override
        public void send(String to, String subject, String html, String plainText) throws IOException {
            Files.createDirectories(outbox);
            String name = FILE_TIME.format(Instant.now()) + "_" + to.replaceAll("[^A-Za-z0-9@._-]", "_") + ".html";
            Path file = outbox.resolve(name);
            Files.writeString(file, html, StandardCharsets.UTF_8);
            log.info("[DEMO EMAIL] To: {} | Subject: {} | Saved to {}", to, subject, file);
        }
    }
}
