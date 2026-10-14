package com.example.ArtConnect.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** An artwork in a user's cart. Adding to the cart doesn't reserve it; checkout does. */
@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    protected CartItem() {}

    public CartItem(User user, Artwork artwork) {
        this.user = user;
        this.artwork = artwork;
        this.addedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Artwork getArtwork() { return artwork; }
    public Instant getAddedAt() { return addedAt; }
}
