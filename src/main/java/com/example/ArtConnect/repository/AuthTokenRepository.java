package com.example.ArtConnect.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.example.ArtConnect.model.AuthToken;
import com.example.ArtConnect.model.User;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {
    @Query("select t.user from AuthToken t where t.tokenHash = :tokenHash and t.expiresAt > :now")
    Optional<User> findActiveUser(String tokenHash, Instant now);

    @Modifying
    @Query("delete from AuthToken t where t.expiresAt <= :now")
    int deleteExpired(Instant now);
}
