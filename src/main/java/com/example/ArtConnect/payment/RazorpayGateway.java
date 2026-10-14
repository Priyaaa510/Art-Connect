package com.example.ArtConnect.payment;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.ArtConnect.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Razorpay (https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/).
 * Uses test or live mode depending on the keys (rzp_test_... / rzp_live_...).
 */
@Component
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "razorpay")
public class RazorpayGateway implements PaymentGateway {
    private static final Logger log = LoggerFactory.getLogger(RazorpayGateway.class);
    private static final String API_BASE_URL = "https://api.razorpay.com";

    private final RestClient restClient;
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final ObjectMapper objectMapper;

    @Autowired
    public RazorpayGateway(RestClient.Builder builder, ObjectMapper objectMapper,
                           @Value("${razorpay.key-id:}") String keyId,
                           @Value("${razorpay.key-secret:}") String keySecret,
                           @Value("${razorpay.webhook-secret:}") String webhookSecret) {
        this(builder, objectMapper, keyId, keySecret, webhookSecret, true);
    }

    /** applyTimeouts is false in tests, whose builder is bound to a mock server. */
    RazorpayGateway(RestClient.Builder builder, ObjectMapper objectMapper, String keyId,
                    String keySecret, String webhookSecret, boolean applyTimeouts) {
        if (applyTimeouts) builder.requestFactory(timeouts());
        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new IllegalStateException(
                    "app.payments.provider=razorpay needs RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET to be set");
        }
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.objectMapper = objectMapper;
        this.restClient = builder
                .baseUrl(API_BASE_URL)
                .defaultHeaders(headers -> headers.setBasicAuth(keyId, keySecret))
                .build();
        log.info("Payments: Razorpay in {} mode", keyId.startsWith("rzp_live_") ? "LIVE" : "test");
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        return factory;
    }

    @Override
    public String name() {
        return "razorpay";
    }

    @Override
    public String publicKey() {
        return keyId;
    }

    @Override
    public String createOrder(long amountInSmallestUnit, String currency, String receipt) {
        try {
            JsonNode response = restClient.post()
                    .uri("/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", amountInSmallestUnit, "currency", currency, "receipt", receipt))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.hasNonNull("id")) {
                throw new IllegalStateException("Razorpay returned no order id");
            }
            return response.get("id").asText();
        } catch (RestClientException | IllegalStateException e) {
            log.error("Could not create Razorpay order for {}", receipt, e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The payment service is unavailable. Please try again.");
        }
    }

    /** Razorpay signs "order_id|payment_id" with the key secret (HMAC-SHA256, hex). */
    @Override
    public boolean verifyPayment(String providerOrderId, String paymentId, String signature) {
        return signatureMatches(providerOrderId + "|" + paymentId, keySecret, signature);
    }

    /** Webhooks are signed over the raw request body with the webhook secret set in the Razorpay dashboard. */
    @Override
    public Optional<PaidOrder> parsePaymentWebhook(String payload, String signature) {
        if (webhookSecret.isBlank()) {
            log.warn("Razorpay webhook received but RAZORPAY_WEBHOOK_SECRET is not set; ignoring it");
            return Optional.empty();
        }
        if (!signatureMatches(payload, webhookSecret, signature)) {
            throw ApiException.badRequest("Invalid webhook signature");
        }
        try {
            JsonNode event = objectMapper.readTree(payload);
            String type = event.path("event").asText();
            if (!type.equals("payment.captured") && !type.equals("order.paid")) {
                return Optional.empty();
            }
            JsonNode payment = event.path("payload").path("payment").path("entity");
            String orderId = payment.path("order_id").asText(null);
            String paymentId = payment.path("id").asText(null);
            return orderId == null || paymentId == null ? Optional.empty() : Optional.of(new PaidOrder(orderId, paymentId));
        } catch (Exception e) {
            throw ApiException.badRequest("Malformed webhook payload");
        }
    }

    /** Asks Razorpay how the payment was made (upi, card, netbanking, wallet...). */
    @Override
    public String paymentMethod(String paymentId, String reportedMethod) {
        try {
            JsonNode payment = restClient.get().uri("/v1/payments/{id}", paymentId).retrieve().body(JsonNode.class);
            String method = payment == null ? "" : payment.path("method").asText("");
            return switch (method) {
                case "upi" -> "UPI";
                case "card" -> "CARD";
                case "netbanking" -> "NETBANKING";
                case "wallet" -> "WALLET";
                default -> "ONLINE";
            };
        } catch (RestClientException e) {
            log.warn("Could not look up the method of Razorpay payment {}", paymentId, e);
            return "ONLINE";
        }
    }

    static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    private static boolean signatureMatches(String data, String secret, String signature) {
        if (signature == null) return false;
        byte[] expected = hmacSha256Hex(data, secret).getBytes(StandardCharsets.UTF_8);
        // Constant-time comparison so the signature can't be guessed byte by byte
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }
}
