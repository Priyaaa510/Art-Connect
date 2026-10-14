package com.example.ArtConnect.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A record of an invoice email sent (or simulated) for a checkout. */
@Entity
@Table(name = "notifications")
public class Notification {
    /** SMS is no longer sent; it remains only so older delivery records can still be read. */
    public enum Channel { EMAIL, SMS }

    /** SIMULATED means the demo outbox stored it instead of a real email provider sending it. */
    public enum Status { SENT, SIMULATED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkout_id")
    private Checkout checkout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Channel channel;

    @Column(nullable = false, length = 254)
    private String recipient;

    @Column(length = 200)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(length = 500)
    private String error;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {}

    public Notification(Checkout checkout, Channel channel, String recipient, String subject,
                        Status status, String provider, String error) {
        this.checkout = checkout;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.status = status;
        this.provider = provider;
        this.error = error == null || error.length() <= 500 ? error : error.substring(0, 500);
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Checkout getCheckout() { return checkout; }
    public Channel getChannel() { return channel; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public Status getStatus() { return status; }
    public String getProvider() { return provider; }
    public String getError() { return error; }
    public Instant getCreatedAt() { return createdAt; }
}
