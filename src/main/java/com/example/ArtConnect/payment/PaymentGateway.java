package com.example.ArtConnect.payment;

import java.util.Optional;

/**
 * An online payment provider. Selected with app.payments.provider: "razorpay" for real payments,
 * or "mock" (the default) for a local demo that charges nothing.
 */
public interface PaymentGateway {

    /** Short provider name stored on orders and sent to the browser, e.g. "razorpay". */
    String name();

    /** The public key the browser checkout needs, or null if the provider has none. */
    String publicKey();

    /** Creates an order at the provider for the amount in the smallest currency unit, returning its id. */
    String createOrder(long amountInSmallestUnit, String currency, String receipt);

    /** Checks the signature the checkout returned to the browser, proving the payment really happened. */
    boolean verifyPayment(String providerOrderId, String paymentId, String signature);

    /** Verifies a server-to-server webhook and extracts the paid order, if the event reports a payment. */
    Optional<PaidOrder> parsePaymentWebhook(String payload, String signature);

    /**
     * How the payment was made: UPI, CARD, NETBANKING or WALLET (ONLINE if unknown). Real gateways look this up
     * themselves; reportedMethod is what the checkout page said and is only trusted by the demo gateway.
     */
    String paymentMethod(String paymentId, String reportedMethod);

    record PaidOrder(String providerOrderId, String paymentId) {}
}
