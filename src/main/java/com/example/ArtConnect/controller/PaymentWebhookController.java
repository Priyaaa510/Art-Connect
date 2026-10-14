package com.example.ArtConnect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ArtConnect.service.CheckoutService;

/**
 * Server-to-server payment notifications. Point the Razorpay dashboard webhook
 * (events: payment.captured, order.paid) at https://your-domain/api/payments/razorpay/webhook.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private final CheckoutService checkoutService;

    public PaymentWebhookController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/razorpay/webhook")
    public ResponseEntity<Void> razorpayWebhook(@RequestBody String payload,
                                                @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        checkoutService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
