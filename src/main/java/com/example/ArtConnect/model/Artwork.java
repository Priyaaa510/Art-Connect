package com.example.ArtConnect.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "artworks")
public class Artwork {
    private static final BigDecimal MIN_INCREMENT = BigDecimal.ONE;
    private static final BigDecimal INCREMENT_RATE = new BigDecimal("0.05");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id")
    private User artist;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    /** The fixed price, or the starting bid for auctions. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 40)
    private String category;

    @ElementCollection
    @CollectionTable(name = "artwork_tags", joinColumns = @JoinColumn(name = "artwork_id"))
    @Column(name = "tag", length = 40)
    private List<String> tags = new ArrayList<>();

    @Column(length = 100)
    private String medium;

    @Column(length = 100)
    private String dimensions;

    @Column(name = "creation_year")
    private Integer year;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false, length = 10)
    private SaleType saleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ArtworkStatus status = ArtworkStatus.AVAILABLE;

    @Column(name = "auction_ends_at")
    private Instant auctionEndsAt;

    @Column(name = "current_bid", precision = 12, scale = 2)
    private BigDecimal currentBid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "highest_bidder_id")
    private User highestBidder;

    @Column(name = "bid_count", nullable = false)
    private int bidCount;

    // Review summary, refreshed whenever a review is added or removed
    @Column(name = "average_rating", nullable = false)
    private double averageRating;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public boolean isAuction() {
        return saleType == SaleType.AUCTION;
    }

    public boolean isAuctionLive(Instant now) {
        return isAuction() && status == ArtworkStatus.AVAILABLE
                && auctionEndsAt != null && auctionEndsAt.isAfter(now);
    }

    public boolean isOwnedBy(User user) {
        return artist.sameAs(user);
    }

    /** The lowest amount the next bid may be. */
    public BigDecimal minimumBid() {
        return currentBid == null ? price : currentBid.add(bidIncrement(currentBid));
    }

    /** Bids must rise by 5% of the current bid, rounded up to a whole unit, and by at least 1. */
    public static BigDecimal bidIncrement(BigDecimal currentBid) {
        BigDecimal fivePercent = currentBid.multiply(INCREMENT_RATE).setScale(0, RoundingMode.CEILING);
        return fivePercent.max(MIN_INCREMENT);
    }

    public void recordBid(User bidder, BigDecimal amount) {
        this.currentBid = amount;
        this.highestBidder = bidder;
        this.bidCount++;
    }

    public Long getId() { return id; }

    public User getArtist() { return artist; }
    public void setArtist(User artist) { this.artist = artist; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }

    public String getMedium() { return medium; }
    public void setMedium(String medium) { this.medium = medium; }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public SaleType getSaleType() { return saleType; }
    public void setSaleType(SaleType saleType) { this.saleType = saleType; }

    public ArtworkStatus getStatus() { return status; }
    public void setStatus(ArtworkStatus status) { this.status = status; }

    public Instant getAuctionEndsAt() { return auctionEndsAt; }
    public void setAuctionEndsAt(Instant auctionEndsAt) { this.auctionEndsAt = auctionEndsAt; }

    public BigDecimal getCurrentBid() { return currentBid; }

    public User getHighestBidder() { return highestBidder; }

    public int getBidCount() { return bidCount; }

    public double getAverageRating() { return averageRating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewSummary(double averageRating, int reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    public Instant getCreatedAt() { return createdAt; }
}
