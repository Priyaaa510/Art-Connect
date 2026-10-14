package com.example.ArtConnect.model;

public enum OrderStatus {
    /** Created at checkout (or when an auction is won); the artwork is reserved until payment. */
    AWAITING_PAYMENT,
    /** Payment verified with the gateway; the artist now ships the artwork. */
    PAID,
    /** The artist has delivered the artwork. */
    COMPLETED,
    /** Cancelled before payment, or the payment window expired. */
    CANCELLED
}
