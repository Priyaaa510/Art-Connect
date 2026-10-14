package com.example.ArtConnect.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ArtConnect.dto.ArtworkDtos.ArtworkResponse;
import com.example.ArtConnect.dto.MarketDtos.CartItemResponse;
import com.example.ArtConnect.dto.MarketDtos.CartResponse;
import com.example.ArtConnect.exception.ApiException;
import com.example.ArtConnect.model.Artwork;
import com.example.ArtConnect.model.ArtworkStatus;
import com.example.ArtConnect.model.CartItem;
import com.example.ArtConnect.model.OrderStatus;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.repository.ArtworkRepository;
import com.example.ArtConnect.repository.CartItemRepository;
import com.example.ArtConnect.repository.OrderRepository;

/**
 * The buyer's cart of fixed-price artworks. Adding to the cart doesn't reserve anything: artworks are only
 * reserved at checkout, so the cart shows which items have been sold to someone else in the meantime.
 */
@Service
public class CartService {
    private final CartItemRepository cartItemRepository;
    private final ArtworkRepository artworkRepository;
    private final OrderRepository orderRepository;

    public CartService(CartItemRepository cartItemRepository, ArtworkRepository artworkRepository,
                       OrderRepository orderRepository) {
        this.cartItemRepository = cartItemRepository;
        this.artworkRepository = artworkRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse cartOf(User user) {
        List<CartItemResponse> items = cartItemRepository.findByUserIdOrderByAddedAtDesc(user.getId()).stream()
                .map(item -> {
                    String problem = problemWith(user, item.getArtwork());
                    return new CartItemResponse(ArtworkResponse.from(item.getArtwork()), problem == null, problem,
                            item.getAddedAt());
                })
                .toList();
        BigDecimal total = items.stream().filter(CartItemResponse::available)
                .map(item -> item.artwork().price()).reduce(BigDecimal.ZERO, BigDecimal::add);
        int available = (int) items.stream().filter(CartItemResponse::available).count();
        return new CartResponse(items, total, available);
    }

    @Transactional
    public CartResponse add(User user, Long artworkId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> ApiException.notFound("Artwork not found"));
        if (artwork.isOwnedBy(user)) {
            throw ApiException.badRequest("You can't buy your own artwork");
        }
        if (artwork.isAuction()) {
            throw ApiException.badRequest("This artwork is sold by auction. Place a bid instead.");
        }
        if (artwork.getStatus() != ArtworkStatus.AVAILABLE) {
            throw ApiException.conflict("Sorry, this artwork is no longer available");
        }
        if (cartItemRepository.findByUserIdAndArtworkId(user.getId(), artworkId).isEmpty()) {
            try {
                cartItemRepository.saveAndFlush(new CartItem(user, artwork));
            } catch (DataIntegrityViolationException e) {
                // Added twice at the same moment (e.g. a double click); it's in the cart either way
            }
        }
        return cartOf(user);
    }

    @Transactional
    public CartResponse remove(User user, Long artworkId) {
        cartItemRepository.findByUserIdAndArtworkId(user.getId(), artworkId).ifPresent(cartItemRepository::delete);
        return cartOf(user);
    }

    /** Why an item in the cart can't be bought right now, or null if it can. */
    private String problemWith(User user, Artwork artwork) {
        if (artwork.isAuction()) {
            return "Now sold by auction";
        }
        return switch (artwork.getStatus()) {
            case AVAILABLE -> null;
            case RESERVED -> orderRepository.existsByArtworkIdAndBuyerIdAndStatus(artwork.getId(), user.getId(),
                    OrderStatus.AWAITING_PAYMENT)
                    ? "Reserved for you: finish paying in My collection"
                    : "Reserved by another buyer";
            case SOLD -> "Sold";
            default -> "No longer available";
        };
    }
}
