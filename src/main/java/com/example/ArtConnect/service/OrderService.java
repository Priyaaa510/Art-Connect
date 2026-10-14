package com.example.ArtConnect.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ArtConnect.dto.MarketDtos.OrderResponse;
import com.example.ArtConnect.exception.ApiException;
import com.example.ArtConnect.model.Artwork;
import com.example.ArtConnect.model.ArtworkStatus;
import com.example.ArtConnect.model.Order;
import com.example.ArtConnect.model.OrderSource;
import com.example.ArtConnect.model.OrderStatus;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.repository.OrderRepository;

/** Order history and lifecycle. Checkout, payment and cancellation live in {@link CheckoutService}. */
@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Called when an auction closes with a winner; the caller holds the artwork lock.
     * The artwork stays reserved for the winner until they check out and pay.
     */
    Order createAuctionOrder(Artwork artwork) {
        artwork.setStatus(ArtworkStatus.RESERVED);
        return orderRepository.save(new Order(artwork, artwork.getHighestBidder(), OrderSource.AUCTION,
                artwork.getCurrentBid()));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> purchasesOf(User buyer) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId()).stream()
                .map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> salesOf(User artist) {
        return orderRepository.findByArtistIdOrderByCreatedAtDesc(artist.getId()).stream()
                .map(OrderResponse::from).toList();
    }

    /** The artist confirms the paid artwork has been delivered. */
    @Transactional
    public OrderResponse complete(User user, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (order.getStatus() != OrderStatus.PAID) {
            throw ApiException.conflict(order.getStatus() == OrderStatus.CANCELLED
                    ? "This order has been cancelled" : "Only paid orders can be marked completed");
        }
        if (!order.getArtist().sameAs(user) && !user.isAdmin()) {
            throw ApiException.forbidden("Only the artist can complete this order");
        }
        order.changeStatus(OrderStatus.COMPLETED);
        return OrderResponse.from(order);
    }
}
