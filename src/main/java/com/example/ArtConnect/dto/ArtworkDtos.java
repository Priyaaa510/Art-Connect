package com.example.ArtConnect.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;

import com.example.ArtConnect.model.Artwork;
import com.example.ArtConnect.model.ArtworkStatus;
import com.example.ArtConnect.model.SaleType;
import com.example.ArtConnect.model.User;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ArtworkDtos {
    private ArtworkDtos() {}

    /**
     * Artwork details sent as multipart form fields on upload, or as JSON on edit.
     * saleType and auctionDurationHours only apply on upload.
     */
    public record ArtworkForm(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 2000) String description,
            @NotNull @DecimalMin(value = "1", message = "must be at least 1")
            @DecimalMax(value = "9999999999", message = "is too large")
            @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotBlank String category,
            @Size(max = 300) String tags,
            @Size(max = 100) String medium,
            @Size(max = 100) String dimensions,
            @Min(1000) @Max(2100) Integer year,
            SaleType saleType,
            @Min(value = 1, message = "must be at least 1 hour")
            @Max(value = 720, message = "must be at most 30 days (720 hours)") Integer auctionDurationHours) {}

    public record ArtworkResponse(
            Long id, String title, String description, BigDecimal price, String category, List<String> tags,
            String medium, String dimensions, Integer year, String imageUrl,
            Long artistId, String artistName, String artistAvatarUrl,
            SaleType saleType, ArtworkStatus status, Instant createdAt,
            Instant auctionEndsAt, boolean auctionLive, BigDecimal currentBid, BigDecimal minimumBid,
            Long highestBidderId, String highestBidderName, int bidCount,
            double averageRating, int reviewCount) {

        public static ArtworkResponse from(Artwork a) {
            User artist = a.getArtist();
            User leader = a.getHighestBidder();
            return new ArtworkResponse(
                    a.getId(), a.getTitle(), a.getDescription(), a.getPrice(), a.getCategory(), List.copyOf(a.getTags()),
                    a.getMedium(), a.getDimensions(), a.getYear(), a.getImageUrl(),
                    artist.getId(), artist.getDisplayName(), artist.getAvatarUrl(),
                    a.getSaleType(), a.getStatus(), a.getCreatedAt(),
                    a.getAuctionEndsAt(), a.isAuctionLive(Instant.now()), a.getCurrentBid(),
                    a.isAuction() ? a.minimumBid() : null,
                    leader == null ? null : leader.getId(), leader == null ? null : leader.getDisplayName(),
                    a.getBidCount(), Math.round(a.getAverageRating() * 10) / 10.0, a.getReviewCount());
        }
    }

    /** Browse filters; every field is optional. */
    public record ArtworkSearch(String q, String category, String tag, SaleType saleType, ArtworkStatus status,
                                Long artistId, BigDecimal minPrice, BigDecimal maxPrice, Boolean liveAuctions,
                                String sort) {}

    public record PageResponse<T>(List<T> items, int page, int size, int totalPages, long totalItems) {
        public static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalPages(), page.getTotalElements());
        }
    }
}
