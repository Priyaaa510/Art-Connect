package com.example.ArtConnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.example.ArtConnect.model.Order;
import com.example.ArtConnect.model.OrderStatus;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Long> {
    /** Locks the order so payment, cancellation and completion can't change it at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(Long id);

    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    List<Order> findByArtistIdOrderByCreatedAtDesc(Long artistId);

    boolean existsByArtworkIdAndBuyerIdAndStatus(Long artworkId, Long buyerId, OrderStatus status);
}
