package com.example.ArtConnect.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.example.ArtConnect.model.Artwork;

import jakarta.persistence.LockModeType;

public interface ArtworkRepository extends JpaRepository<Artwork, Long>, JpaSpecificationExecutor<Artwork> {

    /** Locks the row so concurrent purchases and bids on the same artwork are serialised. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Artwork a where a.id = :id")
    Optional<Artwork> findByIdForUpdate(Long id);

    List<Artwork> findByArtistIdOrderByCreatedAtDesc(Long artistId);

    @Query("select a.id from Artwork a where a.saleType = com.example.ArtConnect.model.SaleType.AUCTION"
            + " and a.status = com.example.ArtConnect.model.ArtworkStatus.AVAILABLE and a.auctionEndsAt <= :now")
    List<Long> findEndedAuctionIds(Instant now);

    @Query("select a.artist.id as artistId, count(a) as artworkCount from Artwork a group by a.artist.id")
    List<ArtistArtworkCount> countByArtist();

    interface ArtistArtworkCount {
        Long getArtistId();
        long getArtworkCount();
    }
}
