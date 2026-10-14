package com.example.ArtConnect.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.ArtConnect.model.Notification;
import com.example.ArtConnect.model.Notification.Channel;
import com.example.ArtConnect.model.Notification.Status;
import com.example.ArtConnect.notification.InvoiceService.InvoiceMessage;
import com.example.ArtConnect.repository.NotificationRepository;
import com.example.ArtConnect.repository.CheckoutRepository;

/**
 * Emails the invoice once a payment is committed. Runs in the background so a slow or failing email
 * provider never affects the payment itself; every attempt is recorded as a Notification.
 */
@Component
public class InvoiceNotifier {
    private static final Logger log = LoggerFactory.getLogger(InvoiceNotifier.class);

    private final InvoiceService invoiceService;
    private final EmailSender emailSender;
    private final NotificationRepository notificationRepository;
    private final CheckoutRepository checkoutRepository;
    private final TransactionTemplate transactionTemplate;

    public InvoiceNotifier(InvoiceService invoiceService, EmailSender emailSender,
                           NotificationRepository notificationRepository, CheckoutRepository checkoutRepository,
                           TransactionTemplate transactionTemplate) {
        this.invoiceService = invoiceService;
        this.emailSender = emailSender;
        this.notificationRepository = notificationRepository;
        this.checkoutRepository = checkoutRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCheckoutPaid(CheckoutPaidEvent event) {
        sendInvoice(event.checkoutId());
    }

    /** Emails the invoice to the billing email saved at checkout. */
    public void sendInvoice(Long checkoutId) {
        InvoiceMessage message;
        try {
            message = invoiceService.messageFor(checkoutId);
        } catch (RuntimeException e) {
            log.error("Could not prepare the invoice for checkout {}", checkoutId, e);
            return;
        }
        if (message.email() == null) return;

        Status status;
        String error = null;
        try {
            emailSender.send(message.email(), message.subject(), message.html(), message.text());
            status = emailSender.simulated() ? Status.SIMULATED : Status.SENT;
        } catch (Exception e) {
            log.warn("Invoice email for checkout {} failed: {}", checkoutId, e.getMessage());
            status = Status.FAILED;
            error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }

        String subject = message.subject().length() <= 200 ? message.subject() : message.subject().substring(0, 200);
        Status finalStatus = status;
        String finalError = error;
        transactionTemplate.executeWithoutResult(tx -> notificationRepository.save(new Notification(
                checkoutRepository.getReferenceById(checkoutId), Channel.EMAIL, message.email(), subject,
                finalStatus, emailSender.provider(), finalError)));
    }
}
