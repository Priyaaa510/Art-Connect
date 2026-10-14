package com.example.ArtConnect.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.ArtConnect.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;

class RazorpayGatewayTest {
    private static final String KEY_ID = "rzp_test_key";
    private static final String KEY_SECRET = "test_secret";
    private static final String WEBHOOK_SECRET = "webhook_secret";

    private MockRestServiceServer razorpay;
    private RazorpayGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        razorpay = MockRestServiceServer.bindTo(builder).build();
        gateway = new RazorpayGateway(builder, new ObjectMapper(), KEY_ID, KEY_SECRET, WEBHOOK_SECRET, false);
    }

    @Test
    void createsOrderWithBasicAuthAndAmountInPaise() {
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((KEY_ID + ":" + KEY_SECRET).getBytes(StandardCharsets.UTF_8));
        razorpay.expect(requestTo("https://api.razorpay.com/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basicAuth))
                .andExpect(content().json("{\"amount\":150000,\"currency\":\"INR\",\"receipt\":\"artconnect_order_7\"}"))
                .andRespond(withSuccess("{\"id\":\"order_ABC123\",\"status\":\"created\"}", MediaType.APPLICATION_JSON));

        assertThat(gateway.createOrder(150000, "INR", "artconnect_order_7")).isEqualTo("order_ABC123");
        assertThat(gateway.publicKey()).isEqualTo(KEY_ID);
        razorpay.verify();
    }

    @Test
    void gatewayErrorsBecomeBadGateway() {
        razorpay.expect(requestTo("https://api.razorpay.com/v1/orders")).andRespond(withServerError());

        assertThatThrownBy(() -> gateway.createOrder(100, "INR", "r"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void verifiesCheckoutSignature() {
        String signature = RazorpayGateway.hmacSha256Hex("order_1|pay_1", KEY_SECRET);

        assertThat(gateway.verifyPayment("order_1", "pay_1", signature)).isTrue();
        assertThat(gateway.verifyPayment("order_1", "pay_2", signature)).isFalse();
        assertThat(gateway.verifyPayment("order_1", "pay_1", "forged")).isFalse();
        assertThat(gateway.verifyPayment("order_1", "pay_1", null)).isFalse();
    }

    @Test
    void parsesSignedPaymentWebhooks() {
        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_9","order_id":"order_9"}}}}""";
        String signature = RazorpayGateway.hmacSha256Hex(payload, WEBHOOK_SECRET);

        assertThat(gateway.parsePaymentWebhook(payload, signature))
                .contains(new PaymentGateway.PaidOrder("order_9", "pay_9"));
        assertThatThrownBy(() -> gateway.parsePaymentWebhook(payload, "forged")).isInstanceOf(ApiException.class);

        String refund = "{\"event\":\"refund.created\",\"payload\":{}}";
        assertThat(gateway.parsePaymentWebhook(refund, RazorpayGateway.hmacSha256Hex(refund, WEBHOOK_SECRET))).isEmpty();
    }

    @Test
    void refusesToStartWithoutKeys() {
        assertThatThrownBy(() -> new RazorpayGateway(RestClient.builder(), new ObjectMapper(), "", "", "", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RAZORPAY_KEY_ID");
    }
}
