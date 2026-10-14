package com.example.ArtConnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ArtConnect.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByCheckoutIdOrderByCreatedAtDesc(Long checkoutId);
}
