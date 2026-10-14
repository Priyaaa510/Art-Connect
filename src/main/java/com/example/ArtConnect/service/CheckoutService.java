package com.example.ArtConnect.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.ArtConnect.dto.MarketDtos.AddressRequest;
import com.example.ArtConnect.dto.MarketDtos.AddressResponse;
import com.example.ArtConnect.dto.MarketDtos.CheckoutDetails;
import com.example.ArtConnect.dto.MarketDtos.CheckoutRequest;
import com.example.ArtConnect.dto.MarketDtos.CheckoutResponse;
import com.example.ArtConnect.dto.MarketDtos.OrderResponse;
import com.example.ArtConnect.dto.MarketDtos.PaymentConfirmation;
import com.example.ArtConnect.exception.ApiException;
import com.example.ArtConnect.model.Artwork;
import com.example.ArtConnect.model.ArtworkStatus;
import com.example.ArtConnect.model.CartItem;
import com.example.ArtConnect.model.Checkout;
import com.example.ArtConnect.model.Order;
import com.example.ArtConnect.model.OrderSource;
import com.example.ArtConnect.model.OrderStatus;
import com.example.ArtConnect.model.SaleType;
import com.example.ArtConnect.model.ShippingAddress;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.notification.CheckoutPaidEvent;
import com.example.ArtConnect.payment.PaymentGateway;
import com.example.ArtConnect.repository.ArtworkRepository;
import com.example.ArtConnect.repository.CartItemRepository;
import com.example.ArtConnect.repository.CheckoutRepository;
import com.example.ArtConnect.repository.OrderRepository;

/**
 * Checkout flow:
 * <ol>
 *   <li>{@link #start} takes the buyer's cart (or a won auction order) plus the shipping address, reserves the
 *       artworks, creates one AWAITING_PAYMENT order per artwork and one gateway order for the total.</li>
 *   <li>The browser opens the gateway and pays.</li>
 *   <li>{@link #confirm} verifies the gateway's signature and marks the checkout and its orders PAID (the webhook
 *       does the same if the browser never reports back). The invoice is then emailed.</li>
 * </ol>
 * Unpaid cart checkouts are cancelled, and their artworks released, after app.payments.hold-minutes.
 */
@Service
public class CheckoutService {
    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    static final String COUNTRY = "India";

    private final CheckoutRepository checkoutRepository;
    private final OrderRepository orderRepository;
    private final ArtworkRepository artworkRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentGateway gateway;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher events;
    private final String currency;
    private final Duration holdDuration;

    public CheckoutService(CheckoutRepository checkoutRepository, OrderRepository orderRepository,
                           ArtworkRepository artworkRepository, CartItemRepository cartItemRepository,
                           PaymentGateway gateway, TransactionTemplate transactionTemplate,
                           ApplicationEventPublisher events,
                           @Value("${app.payments.currency:INR}") String currency,
                           @Value("${app.payments.hold-minutes:30}") long holdMinutes) {
        this.checkoutRepository = checkoutRepository;
        this.orderRepository = orderRepository;
        this.artworkRepository = artworkRepository;
        this.cartItemRepository = cartItemRepository;
        this.gateway = gateway;
        this.transactionTemplate = transactionTemplate;
        this.events = events;
        this.currency = currency;
        this.holdDuration = Duration.ofMinutes(holdMinutes);
    }

    /** Starts paying for the cart (or a won auction): reserves the artworks and opens a payment. */
    @Transactional
    public CheckoutResponse start(User buyer, CheckoutRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        ShippingAddress address = toAddress(request.shippingAddress());
        Checkout checkout = request.orderId() != null
                ? checkoutForWonAuction(buyer, request.orderId(), email, address)
                : checkoutForCart(buyer, request.artworkIds(), email, address);
        return paymentSession(checkout);
    }

    private Checkout checkoutForCart(User buyer, List<Long> pickedArtworkIds, String email, ShippingAddress address) {
        List<CartItem> cart = cartItemRepository.findByUserIdOrderByAddedAtDesc(buyer.getId());
        Set<Long> picked = pickedArtworkIds == null ? null : new HashSet<>(pickedArtworkIds);
        // Lock in id order so two shoppers checking out overlapping carts can't deadlock
        List<Long> artworkIds = cart.stream()
                .map(item -> item.getArtwork().getId())
                .filter(id -> picked == null || picked.contains(id))
                .sorted()
                .toList();
        if (artworkIds.isEmpty()) {
            throw ApiException.badRequest("Your cart is empty");
        }

        List<Artwork> artworks = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        for (Long id : artworkIds) {
            Optional<Artwork> found = artworkRepository.findByIdForUpdate(id);
            if (found.isEmpty()) continue; // deleted since it was added; the cart row is gone too
            Artwork artwork = found.get();
            if (artwork.isOwnedBy(buyer)) {
                throw ApiException.badRequest("You can't buy your own artwork: " + artwork.getTitle());
            }
            if (artwork.isAuction() || artwork.getStatus() != ArtworkStatus.AVAILABLE) {
                unavailable.add(artwork.getTitle());
            } else {
                artworks.add(artwork);
            }
        }
        if (!unavailable.isEmpty()) {
            throw ApiException.conflict("Sorry, these artworks are no longer available: " + String.join(", ", unavailable)
                    + ". Remove them from your cart to continue.");
        }
        if (artworks.isEmpty()) {
            throw ApiException.badRequest("Your cart is empty");
        }

        BigDecimal total = artworks.stream().map(Artwork::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        Checkout checkout = checkoutRepository.save(
                new Checkout(buyer, total, email, address, Instant.now().plus(holdDuration)));
        for (Artwork artwork : artworks) {
            artwork.setStatus(ArtworkStatus.RESERVED);
            Order order = new Order(artwork, buyer, OrderSource.PURCHASE, artwork.getPrice());
            order.attachTo(checkout);
            checkout.getOrders().add(orderRepository.save(order));
        }
        return checkout;
    }

    private Checkout checkoutForWonAuction(User buyer, Long orderId, String email, ShippingAddress address) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        requireBuyer(order.getBuyer(), buyer);
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw ApiException.conflict(order.getStatus() == OrderStatus.CANCELLED
                    ? "This order has been cancelled" : "This order has already been paid");
        }
        Checkout previous = order.getCheckout();
        if (previous != null && previous.getStatus() == Checkout.Status.AWAITING_PAYMENT) {
            // Paying again after closing the payment window: replace the earlier attempt
            previous.cancel();
        }
        // Auction wins are already the winner's; they don't expire like cart reservations
        Checkout checkout = checkoutRepository.save(new Checkout(buyer, order.getPrice(), email, address, null));
        order.attachTo(checkout);
        checkout.getOrders().add(order);
        return checkout;
    }

    /** Re-opens payment for a checkout whose payment window was closed. */
    @Transactional
    public CheckoutResponse resume(User buyer, Long checkoutId) {
        Checkout checkout = lock(checkoutId);
        requireBuyer(checkout.getBuyer(), buyer);
        if (checkout.getStatus() != Checkout.Status.AWAITING_PAYMENT) {
            throw ApiException.conflict(checkout.getStatus() == Checkout.Status.CANCELLED
                    ? "This checkout has been cancelled" : "This checkout has already been paid");
        }
        return paymentSession(checkout);
    }

    /** Verifies the payment the browser reports and marks the checkout paid. */
    @Transactional
    public CheckoutDetails confirm(User buyer, Long checkoutId, PaymentConfirmation confirmation) {
        Checkout checkout = lock(checkoutId);
        requireBuyer(checkout.getBuyer(), buyer);
        if (!confirmation.providerOrderId().equals(checkout.getProviderOrderId())) {
            throw ApiException.badRequest("This payment does not belong to this checkout");
        }
        if (!gateway.verifyPayment(confirmation.providerOrderId(), confirmation.paymentId(), confirmation.signature())) {
            log.warn("Rejected payment {} for checkout {}: signature mismatch", confirmation.paymentId(), checkoutId);
            throw ApiException.badRequest("The payment could not be verified");
        }
        String method = gateway.paymentMethod(confirmation.paymentId(), confirmation.method());
        return details(recordPayment(checkout, confirmation.paymentId(), method));
    }

    /** Handles the gateway's server-to-server notification; safe to receive more than once. */
    public void handleWebhook(String payload, String signature) {
        gateway.parsePaymentWebhook(payload, signature).ifPresent(paid -> {
            Optional<Long> checkoutId = checkoutRepository.findByProviderOrderId(paid.providerOrderId()).map(Checkout::getId);
            if (checkoutId.isEmpty()) {
                log.warn("Webhook for unknown payment order {}", paid.providerOrderId());
                return;
            }
            String method = gateway.paymentMethod(paid.paymentId(), null);
            try {
                transactionTemplate.executeWithoutResult(status ->
                        recordPayment(lock(checkoutId.get()), paid.paymentId(), method));
            } catch (ApiException e) {
                // Acknowledge anyway so the gateway stops retrying; the log explains what needs attention
                log.error("Webhook payment {} for checkout {} not applied: {}", paid.paymentId(), checkoutId.get(), e.getMessage());
            }
        });
    }

    /** The buyer cancels before paying; reserved artworks go back on sale. */
    @Transactional
    public CheckoutDetails cancel(User user, Long checkoutId) {
        Checkout checkout = lock(checkoutId);
        if (!checkout.getBuyer().sameAs(user) && !user.isAdmin()) {
            throw ApiException.forbidden("You can only cancel your own orders");
        }
        if (checkout.getStatus() != Checkout.Status.AWAITING_PAYMENT) {
            throw ApiException.conflict(checkout.getStatus() == Checkout.Status.CANCELLED
                    ? "This checkout has already been cancelled" : "Paid orders can't be cancelled here. Please contact the artist.");
        }
        cancelAndRelease(checkout);
        return details(checkout);
    }

    /** Cancels an unpaid order: its whole checkout if it has one, otherwise just the (won auction) order. */
    @Transactional
    public OrderResponse cancelOrder(User user, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getBuyer().sameAs(user) && !user.isAdmin()) {
            throw ApiException.forbidden("You can only cancel your own orders");
        }
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw ApiException.conflict(order.getStatus() == OrderStatus.CANCELLED
                    ? "This order has been cancelled" : "Paid orders can't be cancelled here. Please contact the artist.");
        }
        Checkout checkout = order.getCheckout();
        if (checkout != null && checkout.getStatus() == Checkout.Status.AWAITING_PAYMENT) {
            cancelAndRelease(lock(checkout.getId()));
        } else {
            cancelOrderAndRelease(order);
        }
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public CheckoutDetails detailsFor(User user, Long checkoutId) {
        Checkout checkout = checkoutRepository.findById(checkoutId)
                .orElseThrow(() -> ApiException.notFound("Checkout not found"));
        if (!checkout.getBuyer().sameAs(user) && !user.isAdmin()) {
            throw ApiException.forbidden("This isn't your checkout");
        }
        return details(checkout);
    }

    /** Releases artworks whose buyers didn't pay in time. Each checkout is handled in its own transaction. */
    @Scheduled(fixedDelayString = "${app.payments.expiry-check-ms:60000}")
    public void releaseExpiredHolds() {
        for (Long id : checkoutRepository.findExpiredHoldIds(Instant.now())) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    Checkout checkout = lock(id);
                    // Re-check under the lock: the payment may have just been confirmed
                    if (checkout.getStatus() == Checkout.Status.AWAITING_PAYMENT && checkout.getHoldExpiresAt() != null
                            && !checkout.getHoldExpiresAt().isAfter(Instant.now())) {
                        cancelAndRelease(checkout);
                        log.info("Released artworks for unpaid checkout {}", id);
                    }
                });
            } catch (RuntimeException e) {
                log.error("Failed to release unpaid checkout {}", id, e);
            }
        }
    }

    private CheckoutResponse paymentSession(Checkout checkout) {
        if (checkout.getProviderOrderId() == null || !gateway.name().equals(checkout.getPaymentProvider())) {
            String providerOrderId = gateway.createOrder(toSmallestUnit(checkout.getTotal()), currency,
                    "artconnect_checkout_" + checkout.getId());
            checkout.attachPaymentOrder(gateway.name(), providerOrderId);
        }
        List<Order> orders = checkout.getOrders();
        String description = orders.size() == 1 ? orders.get(0).getArtworkTitle() : orders.size() + " artworks";
        return new CheckoutResponse(checkout.getId(), gateway.name(), gateway.publicKey(), checkout.getProviderOrderId(),
                toSmallestUnit(checkout.getTotal()), currency, description, checkout.getBuyer().getDisplayName(),
                checkout.getHoldExpiresAt(), checkout.getBillingEmail());
    }

    /**
     * Marks the checkout and its orders paid and the artworks sold, removes them from the cart, and announces it
     * so the invoice is emailed. The caller holds the checkout lock.
     */
    private Checkout recordPayment(Checkout checkout, String paymentId, String method) {
        if (checkout.getStatus() == Checkout.Status.PAID) {
            return checkout; // already recorded (browser and webhook both report the same payment)
        }
        List<Order> orders = checkout.getOrders();
        List<Artwork> artworks = orders.stream()
                .map(Order::getArtwork)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(Artwork::getId))
                .map(a -> artworkRepository.findByIdForUpdate(a.getId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        if (checkout.getStatus() == Checkout.Status.CANCELLED
                && artworks.stream().anyMatch(a -> a.getStatus() != ArtworkStatus.AVAILABLE)) {
            // Paid after the reservation lapsed and someone else got (some of) the artworks
            log.error("REFUND NEEDED: payment {} arrived for cancelled checkout {} whose artworks are no longer available",
                    paymentId, checkout.getId());
            throw ApiException.conflict("Your payment arrived after the reservation expired and some artworks are no "
                    + "longer available. You will be refunded. Payment reference: " + paymentId);
        }
        checkout.markPaid(paymentId, method);
        orders.forEach(order -> order.changeStatus(OrderStatus.PAID));
        artworks.forEach(artwork -> artwork.setStatus(ArtworkStatus.SOLD));
        cartItemRepository.deleteForUser(checkout.getBuyer().getId(), artworks.stream().map(Artwork::getId).toList());
        events.publishEvent(new CheckoutPaidEvent(checkout.getId()));
        return checkout;
    }

    /** Cancels an unpaid checkout and puts its reserved artworks back on sale. The caller holds the checkout lock. */
    void cancelAndRelease(Checkout checkout) {
        checkout.cancel();
        checkout.getOrders().stream()
                .filter(order -> order.getStatus() == OrderStatus.AWAITING_PAYMENT)
                .sorted(Comparator.comparing(Order::getId))
                .forEach(this::cancelOrderAndRelease);
    }

    private void cancelOrderAndRelease(Order order) {
        order.changeStatus(OrderStatus.CANCELLED);
        if (order.getArtwork() != null) {
            artworkRepository.findByIdForUpdate(order.getArtwork().getId())
                    .filter(artwork -> artwork.getStatus() == ArtworkStatus.RESERVED)
                    .ifPresent(artwork -> {
                        artwork.setStatus(ArtworkStatus.AVAILABLE);
                        // A won auction that isn't paid for goes back on sale at its listed price
                        artwork.setSaleType(SaleType.FIXED);
                    });
        }
    }

    private CheckoutDetails details(Checkout checkout) {
        return new CheckoutDetails(checkout.getId(), checkout.getStatus().name(), checkout.getTotal(),
                checkout.getOrders().stream().map(OrderResponse::from).toList(), checkout.getBillingEmail(),
                AddressResponse.from(checkout.getShippingAddress()), checkout.getPaymentId(), checkout.getPaymentMethod(),
                checkout.getPaidAt(), checkout.getStatus() == Checkout.Status.AWAITING_PAYMENT ? checkout.getHoldExpiresAt() : null,
                checkout.getInvoiceNumber(), checkout.getCreatedAt());
    }

    private Checkout lock(Long checkoutId) {
        return checkoutRepository.findByIdForUpdate(checkoutId).orElseThrow(() -> ApiException.notFound("Checkout not found"));
    }

    private static void requireBuyer(User owner, User user) {
        if (!owner.sameAs(user)) {
            throw ApiException.forbidden("This isn't your order");
        }
    }

    private static ShippingAddress toAddress(AddressRequest a) {
        return new ShippingAddress(a.fullName().trim(), a.line1().trim(), blankToNull(a.line2()), a.city().trim(),
                a.state().trim(), a.postalCode().trim(), COUNTRY);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Rupees to paise (gateways take integer amounts in the smallest unit). */
    static long toSmallestUnit(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
