package com.example.ArtConnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.ArtConnect.model.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByArtworkIdOrderByCreatedAtDesc(Long artworkId);

    boolean existsByArtworkIdAndAuthorId(Long artworkId, Long authorId);

    @Query("select coalesce(avg(r.rating), 0) as average, count(r) as count from Review r where r.artwork.id = :artworkId")
    RatingSummary summarizeArtwork(Long artworkId);

    @Query("select coalesce(avg(r.rating), 0) as average, count(r) as count from Review r where r.artwork.artist.id = :artistId")
    RatingSummary summarizeArtist(Long artistId);

    interface RatingSummary {
        double getAverage();
        long getCount();
    }
}
