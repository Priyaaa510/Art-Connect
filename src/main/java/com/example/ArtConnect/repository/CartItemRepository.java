package com.example.ArtConnect.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.example.ArtConnect.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserIdOrderByAddedAtDesc(Long userId);

    Optional<CartItem> findByUserIdAndArtworkId(Long userId, Long artworkId);

    @Modifying
    @Query("delete from CartItem c where c.user.id = :userId and c.artwork.id in :artworkIds")
    int deleteForUser(Long userId, Collection<Long> artworkIds);
}
