package com.example.ArtConnect.notification;

/** Published when a checkout's payment has been verified; the invoice is emailed once the transaction commits. */
public record CheckoutPaidEvent(Long checkoutId) {}
