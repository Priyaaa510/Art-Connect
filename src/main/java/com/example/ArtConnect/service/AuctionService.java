package com.example.ArtConnect.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.ArtConnect.dto.ArtworkDtos.ArtworkResponse;
import com.example.ArtConnect.dto.MarketDtos.BidResponse;
import com.example.ArtConnect.dto.MarketDtos.MyBidResponse;
import com.example.ArtConnect.exception.ApiException;
import com.example.ArtConnect.model.Artwork;
import com.example.ArtConnect.model.ArtworkStatus;
import com.example.ArtConnect.model.Bid;
import com.example.ArtConnect.model.SaleType;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.repository.ArtworkRepository;
import com.example.ArtConnect.repository.BidRepository;
import com.example.ArtConnect.repository.BidRepository.BidderArtworkBid;

@Service
public class AuctionService {
    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);
    /** A bid in the final minutes pushes the end back, so nobody can win by sniping. */
    static final Duration ANTI_SNIPING_WINDOW = Duration.ofMinutes(2);

    private final ArtworkRepository artworkRepository;
    private final BidRepository bidRepository;
    private final OrderService orderService;
    private final TransactionTemplate transactionTemplate;

    public AuctionService(ArtworkRepository artworkRepository, BidRepository bidRepository,
                          OrderService orderService, TransactionTemplate transactionTemplate) {
        this.artworkRepository = artworkRepository;
        this.bidRepository = bidRepository;
        this.orderService = orderService;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public ArtworkResponse placeBid(User bidder, Long artworkId, BigDecimal amount) {
        Artwork artwork = artworkRepository.findByIdForUpdate(artworkId)
                .orElseThrow(() -> ApiException.notFound("Artwork not found"));
        Instant now = Instant.now();
        if (!artwork.isAuction()) {
            throw ApiException.badRequest("This artwork has a fixed price. Buy it directly instead.");
        }
        if (!artwork.isAuctionLive(now)) {
            throw ApiException.conflict("This auction has ended");
        }
        if (artwork.isOwnedBy(bidder)) {
            throw ApiException.badRequest("You can't bid on your own artwork");
        }
        if (bidder.sameAs(artwork.getHighestBidder())) {
            throw ApiException.conflict("You already have the highest bid");
        }
        BigDecimal bid = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal minimum = artwork.minimumBid();
        if (bid.compareTo(minimum) < 0) {
            throw ApiException.badRequest("Your bid must be at least " + minimum.toPlainString());
        }

        artwork.recordBid(bidder, bid);
        Instant extendedEnd = now.plus(ANTI_SNIPING_WINDOW);
        if (artwork.getAuctionEndsAt().isBefore(extendedEnd)) {
            artwork.setAuctionEndsAt(extendedEnd);
        }
        bidRepository.save(new Bid(artwork, bidder, bid));
        return ArtworkResponse.from(artwork);
    }

    @Transactional(readOnly = true)
    public List<BidResponse> history(Long artworkId) {
        if (!artworkRepository.existsById(artworkId)) throw ApiException.notFound("Artwork not found");
        return bidRepository.findTop50ByArtworkIdOrderByAmountDesc(artworkId).stream()
                .map(BidResponse::from).toList();
    }

    /** Every auction the user has bid on: live ones first (ending soonest), then finished ones. */
    @Transactional(readOnly = true)
    public List<MyBidResponse> bidsOf(User user) {
        Map<Long, BigDecimal> myHighest = bidRepository.findHighestBidsByBidder(user.getId()).stream()
                .collect(Collectors.toMap(BidderArtworkBid::getArtworkId, BidderArtworkBid::getHighestAmount));
        Map<Long, Artwork> artworks = artworkRepository.findAllById(myHighest.keySet()).stream()
                .collect(Collectors.toMap(Artwork::getId, Function.identity()));
        Instant now = Instant.now();
        return artworks.values().stream()
                .sorted(Comparator.comparing((Artwork a) -> !a.isAuctionLive(now))
                        .thenComparing(Artwork::getAuctionEndsAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(a -> {
                    boolean leading = user.sameAs(a.getHighestBidder());
                    boolean won = leading && a.getStatus() != ArtworkStatus.AVAILABLE;
                    return new MyBidResponse(ArtworkResponse.from(a), myHighest.get(a.getId()), leading, won);
                })
                .toList();
    }

    /** Settles auctions whose end time has passed. Each one closes in its own transaction. */
    @Scheduled(fixedDelayString = "${app.auctions.close-interval-ms:30000}")
    public void closeEndedAuctions() {
        for (Long id : artworkRepository.findEndedAuctionIds(Instant.now())) {
            try {
                transactionTemplate.executeWithoutResult(status -> closeAuction(id));
            } catch (RuntimeException e) {
                log.error("Failed to close auction for artwork {}", id, e);
            }
        }
    }

    private void closeAuction(Long artworkId) {
        Artwork artwork = artworkRepository.findByIdForUpdate(artworkId).orElse(null);
        Instant now = Instant.now();
        // Re-check under the lock: a last-second bid may have extended it, or it may already be settled
        if (artwork == null || !artwork.isAuction() || artwork.getStatus() != ArtworkStatus.AVAILABLE
                || artwork.getAuctionEndsAt().isAfter(now)) {
            return;
        }
        if (artwork.getHighestBidder() != null) {
            orderService.createAuctionOrder(artwork);
            log.info("Auction for artwork {} won at {}", artworkId, artwork.getCurrentBid());
        } else {
            // No bids: keep it on sale as a fixed-price listing at the starting price
            artwork.setSaleType(SaleType.FIXED);
            log.info("Auction for artwork {} ended without bids; relisted at a fixed price", artworkId);
        }
    }
}
