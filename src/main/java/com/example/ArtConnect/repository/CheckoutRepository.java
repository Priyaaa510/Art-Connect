package com.example.ArtConnect.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.example.ArtConnect.model.Checkout;

import jakarta.persistence.LockModeType;

public interface CheckoutRepository extends JpaRepository<Checkout, Long> {
    /** Locks the checkout so payment, cancellation and expiry can't change it at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Checkout c where c.id = :id")
    Optional<Checkout> findByIdForUpdate(Long id);

    Optional<Checkout> findByProviderOrderId(String providerOrderId);

    @Query("select c.id from Checkout c where c.status = com.example.ArtConnect.model.Checkout.Status.AWAITING_PAYMENT"
            + " and c.holdExpiresAt <= :now")
    List<Long> findExpiredHoldIds(Instant now);
}
