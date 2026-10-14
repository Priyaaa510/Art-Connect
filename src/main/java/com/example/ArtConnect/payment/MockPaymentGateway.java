package com.example.ArtConnect.payment;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Demo payments for local development: no money moves and every payment "succeeds".
 * Never use this in production. Set PAYMENT_PROVIDER=razorpay with real keys instead.
 */
@Component
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {
    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);
    static final String ORDER_PREFIX = "mock_order_";
    static final String PAYMENT_PREFIX = "mock_pay_";

    public MockPaymentGateway() {
        log.warn("Payments: DEMO mode (PAYMENT_PROVIDER=mock). No real money is charged. "
                + "Set PAYMENT_PROVIDER=razorpay with Razorpay keys before going live.");
    }

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public String publicKey() {
        return null;
    }

    @Override
    public String createOrder(long amountInSmallestUnit, String currency, String receipt) {
        return ORDER_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public boolean verifyPayment(String providerOrderId, String paymentId, String signature) {
        return providerOrderId.startsWith(ORDER_PREFIX) && paymentId.startsWith(PAYMENT_PREFIX);
    }

    @Override
    public Optional<PaidOrder> parsePaymentWebhook(String payload, String signature) {
        return Optional.empty();
    }

    @Override
    public String paymentMethod(String paymentId, String reportedMethod) {
        return reportedMethod == null ? "UPI" : reportedMethod;
    }
}
