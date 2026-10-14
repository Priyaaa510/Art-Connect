package com.example.ArtConnect.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** One payment for one or more orders, with the shipping address and the invoice. */
@Entity
@Table(name = "checkouts")
public class Checkout {
    public enum Status { AWAITING_PAYMENT, PAID, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @OneToMany(mappedBy = "checkout")
    @OrderBy("id")
    private List<Order> orders = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.AWAITING_PAYMENT;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "billing_email", length = 254)
    private String billingEmail;

    @Embedded
    private ShippingAddress shippingAddress;

    @Column(name = "payment_provider", length = 20)
    private String paymentProvider;

    /** The gateway's order id (e.g. Razorpay "order_..."). */
    @Column(name = "provider_order_id", length = 64, unique = true)
    private String providerOrderId;

    @Column(name = "payment_id", length = 64)
    private String paymentId;

    /** UPI, CARD, NETBANKING, WALLET, or ONLINE when the gateway doesn't say. */
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "paid_at")
    private Instant paidAt;

    /** Unpaid checkouts are cancelled and their artworks released after this time; null means no expiry. */
    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    @Column(name = "invoice_number", length = 30, unique = true)
    private String invoiceNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Checkout() {}

    public Checkout(User buyer, BigDecimal total, String billingEmail, ShippingAddress shippingAddress,
                    Instant holdExpiresAt) {
        this.buyer = buyer;
        this.total = total;
        this.billingEmail = billingEmail;
        this.shippingAddress = shippingAddress;
        this.holdExpiresAt = holdExpiresAt;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void attachPaymentOrder(String provider, String providerOrderId) {
        this.paymentProvider = provider;
        this.providerOrderId = providerOrderId;
        touch();
    }

    /** Records the verified payment and issues the invoice number, e.g. INV-2026-000042. */
    public void markPaid(String paymentId, String paymentMethod) {
        this.paymentId = paymentId;
        this.paymentMethod = paymentMethod;
        this.paidAt = Instant.now();
        this.holdExpiresAt = null;
        this.invoiceNumber = "INV-%d-%06d".formatted(paidAt.atZone(ZoneOffset.UTC).getYear(), id);
        this.status = Status.PAID;
        touch();
    }

    public void cancel() {
        this.status = Status.CANCELLED;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getBuyer() { return buyer; }
    public List<Order> getOrders() { return orders; }
    public Status getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public String getBillingEmail() { return billingEmail; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public String getPaymentProvider() { return paymentProvider; }
    public String getProviderOrderId() { return providerOrderId; }
    public String getPaymentId() { return paymentId; }
    public String getPaymentMethod() { return paymentMethod; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public Instant getCreatedAt() { return createdAt; }
}
