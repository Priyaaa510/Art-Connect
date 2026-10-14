package com.example.ArtConnect.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ArtConnect.dto.MarketDtos.ReviewRequest;
import com.example.ArtConnect.dto.MarketDtos.ReviewResponse;
import com.example.ArtConnect.exception.ApiException;
import com.example.ArtConnect.model.Artwork;
import com.example.ArtConnect.model.Review;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.repository.ArtworkRepository;
import com.example.ArtConnect.repository.ReviewRepository;
import com.example.ArtConnect.repository.ReviewRepository.RatingSummary;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ArtworkRepository artworkRepository;

    public ReviewService(ReviewRepository reviewRepository, ArtworkRepository artworkRepository) {
        this.reviewRepository = reviewRepository;
        this.artworkRepository = artworkRepository;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> forArtwork(Long artworkId) {
        if (!artworkRepository.existsById(artworkId)) throw ApiException.notFound("Artwork not found");
        return reviewRepository.findByArtworkIdOrderByCreatedAtDesc(artworkId).stream()
                .map(ReviewResponse::from).toList();
    }

    @Transactional
    public ReviewResponse create(User author, Long artworkId, ReviewRequest request) {
        Artwork artwork = artworkRepository.findByIdForUpdate(artworkId)
                .orElseThrow(() -> ApiException.notFound("Artwork not found"));
        if (artwork.isOwnedBy(author)) {
            throw ApiException.badRequest("You can't review your own artwork");
        }
        if (reviewRepository.existsByArtworkIdAndAuthorId(artworkId, author.getId())) {
            throw ApiException.conflict("You have already reviewed this artwork");
        }
        Review review = reviewRepository.saveAndFlush(
                new Review(artwork, author, request.rating(), request.comment().trim()));
        refreshSummary(artwork);
        return ReviewResponse.from(review);
    }

    @Transactional
    public void delete(User user, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Review not found"));
        if (!review.getAuthor().sameAs(user) && !user.isAdmin()) {
            throw ApiException.forbidden("You can only delete your own reviews");
        }
        Artwork artwork = artworkRepository.findByIdForUpdate(review.getArtwork().getId()).orElseThrow();
        reviewRepository.delete(review);
        reviewRepository.flush();
        refreshSummary(artwork);
    }

    @Transactional(readOnly = true)
    public RatingSummary summaryForArtist(Long artistId) {
        return reviewRepository.summarizeArtist(artistId);
    }

    private void refreshSummary(Artwork artwork) {
        RatingSummary summary = reviewRepository.summarizeArtwork(artwork.getId());
        artwork.setReviewSummary(summary.getAverage(), (int) summary.getCount());
    }
}
