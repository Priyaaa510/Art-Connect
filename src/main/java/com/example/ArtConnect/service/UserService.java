package com.example.ArtConnect.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.ArtConnect.dto.UserDtos.ArtistProfile;
import com.example.ArtConnect.dto.UserDtos.ArtistSummary;
import com.example.ArtConnect.dto.UserDtos.ProfileUpdateRequest;
import com.example.ArtConnect.dto.UserDtos.UserProfile;
import com.example.ArtConnect.exception.ApiException;
import com.example.ArtConnect.model.Role;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.repository.ArtworkRepository;
import com.example.ArtConnect.repository.ArtworkRepository.ArtistArtworkCount;
import com.example.ArtConnect.repository.ReviewRepository.RatingSummary;
import com.example.ArtConnect.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final ArtworkRepository artworkRepository;
    private final ArtworkService artworkService;
    private final ExhibitionService exhibitionService;
    private final ReviewService reviewService;
    private final CloudinaryService cloudinaryService;

    public UserService(UserRepository userRepository, ArtworkRepository artworkRepository,
                       ArtworkService artworkService, ExhibitionService exhibitionService,
                       ReviewService reviewService, CloudinaryService cloudinaryService) {
        this.userRepository = userRepository;
        this.artworkRepository = artworkRepository;
        this.artworkService = artworkService;
        this.exhibitionService = exhibitionService;
        this.reviewService = reviewService;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional
    public UserProfile updateProfile(User current, ProfileUpdateRequest request) {
        User user = reload(current);
        user.setDisplayName(request.displayName().trim());
        user.setBio(blankToNull(request.bio()));
        user.setLocation(blankToNull(request.location()));
        user.setWebsite(blankToNull(request.website()));
        if (request.becomeArtist() && user.getRole() == Role.COLLECTOR) {
            user.setRole(Role.ARTIST);
        }
        return UserProfile.from(user);
    }

    @Transactional
    public UserProfile updateAvatar(User current, MultipartFile image) {
        User user = reload(current);
        user.setAvatarUrl(cloudinaryService.uploadImage(image, "artconnect/avatars"));
        return UserProfile.from(user);
    }

    @Transactional(readOnly = true)
    public List<ArtistSummary> artists() {
        Map<Long, Long> counts = artworkRepository.countByArtist().stream()
                .collect(Collectors.toMap(ArtistArtworkCount::getArtistId, ArtistArtworkCount::getArtworkCount));
        return userRepository.findByRoleOrderByDisplayNameAsc(Role.ARTIST).stream()
                .map(u -> new ArtistSummary(u.getId(), u.getUsername(), u.getDisplayName(), u.getAvatarUrl(),
                        u.getLocation(), u.getBio(), counts.getOrDefault(u.getId(), 0L)))
                .sorted(Comparator.comparingLong(ArtistSummary::artworkCount).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public ArtistProfile artistProfile(Long id) {
        User artist = userRepository.findById(id)
                .filter(User::canSell)
                .orElseThrow(() -> ApiException.notFound("Artist not found"));
        RatingSummary rating = reviewService.summaryForArtist(id);
        return new ArtistProfile(UserProfile.from(artist), artworkService.byArtist(id),
                exhibitionService.byCurator(id), Math.round(rating.getAverage() * 10) / 10.0, rating.getCount());
    }

    private User reload(User user) {
        return userRepository.findById(user.getId()).orElseThrow(() -> ApiException.unauthorized("Please log in again"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
