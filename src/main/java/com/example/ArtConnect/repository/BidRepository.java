package com.example.ArtConnect.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.ArtConnect.model.Bid;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findTop50ByArtworkIdOrderByAmountDesc(Long artworkId);

    @Query("select b.artwork.id as artworkId, max(b.amount) as highestAmount from Bid b"
            + " where b.bidder.id = :bidderId group by b.artwork.id")
    List<BidderArtworkBid> findHighestBidsByBidder(Long bidderId);

    interface BidderArtworkBid {
        Long getArtworkId();
        BigDecimal getHighestAmount();
    }
}
