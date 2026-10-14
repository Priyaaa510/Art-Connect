package com.example.ArtConnect.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.example.ArtConnect.dto.ArtworkDtos.ArtworkResponse;
import com.example.ArtConnect.model.Bid;
import com.example.ArtConnect.model.Checkout;
import com.example.ArtConnect.model.Notification;
import com.example.ArtConnect.model.Order;
import com.example.ArtConnect.model.OrderSource;
import com.example.ArtConnect.model.OrderStatus;
import com.example.ArtConnect.model.Review;
import com.example.ArtConnect.model.ShippingAddress;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Orders, bids and reviews. */
public final class MarketDtos {
    private MarketDtos() {}

    /** One artwork in an order. Payment, shipping and invoice details come from its checkout, if it has one. */
    public record OrderResponse(Long id, Long artworkId, String artworkTitle, String imageUrl, BigDecimal price,
                                OrderSource source, OrderStatus status, Long buyerId, String buyerName,
                                Long artistId, String artistName, Instant createdAt, Instant updatedAt,
                                Long checkoutId, String paymentId, Instant paidAt, Instant holdExpiresAt,
                                String paymentMethod, String invoiceNumber, String billingEmail,
                                AddressResponse shippingAddress) {
        public static OrderResponse from(Order o) {
            Checkout c = o.getCheckout();
            boolean awaiting = c != null && c.getStatus() == Checkout.Status.AWAITING_PAYMENT;
            return new OrderResponse(o.getId(), o.getArtwork() == null ? null : o.getArtwork().getId(),
                    o.getArtworkTitle(), o.getImageUrl(), o.getPrice(), o.getSource(), o.getStatus(),
                    o.getBuyer().getId(), o.getBuyer().getDisplayName(),
                    o.getArtist().getId(), o.getArtist().getDisplayName(), o.getCreatedAt(), o.getUpdatedAt(),
                    c == null ? null : c.getId(),
                    c == null ? null : c.getPaymentId(),
                    c == null ? null : c.getPaidAt(),
                    awaiting ? c.getHoldExpiresAt() : null,
                    c == null ? null : c.getPaymentMethod(),
                    c == null ? null : c.getInvoiceNumber(),
                    c == null ? null : c.getBillingEmail(),
                    c == null ? null : AddressResponse.from(c.getShippingAddress()));
        }
    }

    /** Shipping address entered at checkout (India). */
    public record AddressRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Size(max = 200) String line1,
            @Size(max = 200) String line2,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Pattern(regexp = "^[1-9][0-9]{5}$", message = "must be a valid 6-digit PIN code") String postalCode) {}

    public record AddressResponse(String fullName, String line1, String line2, String city, String state,
                                  String postalCode, String country) {
        public static AddressResponse from(ShippingAddress a) {
            return a == null || a.getFullName() == null ? null : new AddressResponse(a.getFullName(), a.getLine1(),
                    a.getLine2(), a.getCity(), a.getState(), a.getPostalCode(), a.getCountry());
        }
    }

    /**
     * Starts a checkout: either the buyer's cart (artworkIds picks some of it; null means all of it),
     * or orderId to pay for a won auction.
     */
    public record CheckoutRequest(
            @NotBlank @Email(message = "must be a valid email address") @Size(max = 254) String email,
            @NotNull @Valid AddressRequest shippingAddress,
            @Size(max = 50) List<Long> artworkIds,
            Long orderId) {}

    /**
     * Everything the browser needs to open the payment gateway for a checkout.
     * amount is in the smallest currency unit (paise for INR), as gateways expect.
     */
    public record CheckoutResponse(Long checkoutId, String provider, String keyId, String providerOrderId,
                                   long amount, String currency, String description, String buyerName,
                                   Instant holdExpiresAt, String email) {}

    /** A checkout with its items, as shown on the confirmation and in the buyer's orders. */
    public record CheckoutDetails(Long id, String status, BigDecimal total, List<OrderResponse> items,
                                  String billingEmail, AddressResponse shippingAddress, String paymentId,
                                  String paymentMethod, Instant paidAt, Instant holdExpiresAt,
                                  String invoiceNumber, Instant createdAt) {}

    public record AddToCartRequest(@NotNull Long artworkId) {}

    /** available is false when the artwork has been sold, reserved by someone else or withdrawn. */
    public record CartItemResponse(ArtworkResponse artwork, boolean available, String problem, Instant addedAt) {}

    /** total counts only the items that can still be bought. */
    public record CartResponse(List<CartItemResponse> items, BigDecimal total, int availableCount) {}

    /** What the gateway's checkout returns to the browser after a successful payment. */
    public record PaymentConfirmation(@NotBlank @Size(max = 64) String providerOrderId,
                                      @NotBlank @Size(max = 64) String paymentId,
                                      @NotBlank @Size(max = 256) String signature,
                                      @Pattern(regexp = "UPI|CARD|NETBANKING|WALLET") String method) {}

    public record NotificationResponse(String channel, String recipient, String status, String provider,
                                       String error, Instant createdAt) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getChannel().name(), n.getRecipient(), n.getStatus().name(),
                    n.getProvider(), n.getError(), n.getCreatedAt());
        }
    }

    /**
     * Everything shown on an invoice, plus where it was emailed. A seller viewing it (sellerView) only
     * sees their own items and their subtotal.
     */
    public record InvoiceResponse(String invoiceNumber, Instant issuedAt, Long checkoutId, Long buyerId,
                                  String buyerName, String billingEmail, AddressResponse shippingAddress,
                                  List<OrderResponse> items, BigDecimal total, String paymentMethod,
                                  String paymentId, String currency, boolean sellerView,
                                  List<NotificationResponse> notifications) {}

    public record BidRequest(@NotNull @DecimalMin("1") @Digits(integer = 10, fraction = 2) BigDecimal amount) {}

    public record BidResponse(Long id, BigDecimal amount, Long bidderId, String bidderName, Instant createdAt) {
        public static BidResponse from(Bid b) {
            return new BidResponse(b.getId(), b.getAmount(), b.getBidder().getId(),
                    b.getBidder().getDisplayName(), b.getCreatedAt());
        }
    }

    /** An auction the current user has bid on, with how they stand. */
    public record MyBidResponse(ArtworkResponse artwork, BigDecimal myHighestBid, boolean leading, boolean won) {}

    public record ReviewRequest(@Min(1) @Max(5) int rating, @NotBlank @Size(max = 1000) String comment) {}

    public record ReviewResponse(Long id, Long artworkId, int rating, String comment, Long authorId,
                                 String authorName, String authorAvatarUrl, Instant createdAt) {
        public static ReviewResponse from(Review r) {
            return new ReviewResponse(r.getId(), r.getArtwork().getId(), r.getRating(), r.getComment(),
                    r.getAuthor().getId(), r.getAuthor().getDisplayName(), r.getAuthor().getAvatarUrl(),
                    r.getCreatedAt());
        }
    }
}
