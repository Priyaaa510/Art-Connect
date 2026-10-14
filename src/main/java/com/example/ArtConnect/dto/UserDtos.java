package com.example.ArtConnect.dto;

import java.time.Instant;
import java.util.List;

import com.example.ArtConnect.model.Role;
import com.example.ArtConnect.model.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {
    private UserDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 30)
            @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "may only contain letters, numbers, dots, dashes and underscores")
            String username,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotNull Role role,
            @Size(max = 60) String displayName) {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record AuthResponse(String token, UserProfile user) {}

    public record ProfileUpdateRequest(
            @NotBlank @Size(max = 60) String displayName,
            @Size(max = 1000) String bio,
            @Size(max = 100) String location,
            @Size(max = 200) @Pattern(regexp = "^(https?://\\S+)?$", message = "must start with http:// or https://")
            String website,
            boolean becomeArtist) {}

    public record UserProfile(Long id, String username, String displayName, Role role, String bio,
                              String avatarUrl, String location, String website, Instant createdAt) {
        public static UserProfile from(User user) {
            return new UserProfile(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(),
                    user.getBio(), user.getAvatarUrl(), user.getLocation(), user.getWebsite(), user.getCreatedAt());
        }
    }

    public record ArtistSummary(Long id, String username, String displayName, String avatarUrl,
                                String location, String bio, long artworkCount) {}

    public record ArtistProfile(UserProfile artist, List<ArtworkDtos.ArtworkResponse> artworks,
                                List<ExhibitionDtos.ExhibitionSummary> exhibitions,
                                double averageRating, long reviewCount) {}
}
