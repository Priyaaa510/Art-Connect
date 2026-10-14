package com.example.ArtConnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ArtConnect.model.Role;
import com.example.ArtConnect.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRoleOrderByDisplayNameAsc(Role role);
}
