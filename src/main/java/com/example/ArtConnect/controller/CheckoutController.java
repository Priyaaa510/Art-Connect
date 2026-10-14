package com.example.ArtConnect.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.ArtConnect.dto.MarketDtos.CheckoutDetails;
import com.example.ArtConnect.dto.MarketDtos.CheckoutRequest;
import com.example.ArtConnect.dto.MarketDtos.CheckoutResponse;
import com.example.ArtConnect.dto.MarketDtos.InvoiceResponse;
import com.example.ArtConnect.dto.MarketDtos.PaymentConfirmation;
import com.example.ArtConnect.exception.ApiException;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.notification.InvoiceNotifier;
import com.example.ArtConnect.notification.InvoiceService;
import com.example.ArtConnect.security.CurrentUser;
import com.example.ArtConnect.service.CheckoutService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/checkouts")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final InvoiceService invoiceService;
    private final InvoiceNotifier invoiceNotifier;

    public CheckoutController(CheckoutService checkoutService, InvoiceService invoiceService,
                              InvoiceNotifier invoiceNotifier) {
        this.checkoutService = checkoutService;
        this.invoiceService = invoiceService;
        this.invoiceNotifier = invoiceNotifier;
    }

    /** Checks out the cart (or a won auction) to the given address and opens a payment for it. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutResponse start(@CurrentUser User user, @Valid @RequestBody CheckoutRequest request) {
        return checkoutService.start(user, request);
    }

    @GetMapping("/{checkoutId}")
    public CheckoutDetails details(@CurrentUser User user, @PathVariable Long checkoutId) {
        return checkoutService.detailsFor(user, checkoutId);
    }

    /** Re-opens payment for an unpaid checkout, e.g. after the payment window was closed. */
    @PostMapping("/{checkoutId}/pay")
    public CheckoutResponse pay(@CurrentUser User user, @PathVariable Long checkoutId) {
        return checkoutService.resume(user, checkoutId);
    }

    /** The browser reports a completed payment; the server verifies it with the gateway's signature. */
    @PostMapping("/{checkoutId}/payment")
    public CheckoutDetails confirmPayment(@CurrentUser User user, @PathVariable Long checkoutId,
                                          @Valid @RequestBody PaymentConfirmation confirmation) {
        return checkoutService.confirm(user, checkoutId, confirmation);
    }

    @PostMapping("/{checkoutId}/cancel")
    public CheckoutDetails cancel(@CurrentUser User user, @PathVariable Long checkoutId) {
        return checkoutService.cancel(user, checkoutId);
    }

    /** The invoice of a paid checkout, with where it was emailed. Buyer, seller (their items only) or admin. */
    @GetMapping("/{checkoutId}/invoice")
    public InvoiceResponse invoice(@CurrentUser User user, @PathVariable Long checkoutId) {
        return invoiceService.invoiceFor(user, checkoutId);
    }

    /** Emails the invoice to the billing email again. Buyer or admin only. */
    @PostMapping("/{checkoutId}/invoice/send")
    public InvoiceResponse resendInvoice(@CurrentUser User user, @PathVariable Long checkoutId) {
        InvoiceResponse invoice = invoiceService.invoiceFor(user, checkoutId);
        if (!user.getId().equals(invoice.buyerId()) && !user.isAdmin()) {
            throw ApiException.forbidden("Only the buyer can resend this invoice");
        }
        invoiceNotifier.sendInvoice(checkoutId);
        return invoiceService.invoiceFor(user, checkoutId);
    }
}
