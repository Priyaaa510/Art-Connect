package com.example.ArtConnect.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.ArtConnect.dto.ArtworkDtos.ArtworkForm;
import com.example.ArtConnect.dto.ArtworkDtos.ArtworkResponse;
import com.example.ArtConnect.dto.ArtworkDtos.ArtworkSearch;
import com.example.ArtConnect.dto.ArtworkDtos.PageResponse;
import com.example.ArtConnect.exception.ApiException;
import com.example.ArtConnect.model.Artwork;
import com.example.ArtConnect.model.ArtworkStatus;
import com.example.ArtConnect.model.Categories;
import com.example.ArtConnect.model.Exhibition;
import com.example.ArtConnect.model.SaleType;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.repository.ArtworkRepository;
import com.example.ArtConnect.repository.ExhibitionRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

@Service
public class ArtworkService {
    private static final int MAX_TAGS = 10;
    private static final int MAX_TAG_LENGTH = 40;
    private static final int MAX_PAGE_SIZE = 48;

    private final ArtworkRepository artworkRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final CloudinaryService cloudinaryService;

    public ArtworkService(ArtworkRepository artworkRepository, ExhibitionRepository exhibitionRepository,
                          CloudinaryService cloudinaryService) {
        this.artworkRepository = artworkRepository;
        this.exhibitionRepository = exhibitionRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ArtworkResponse> search(ArtworkSearch search, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                sortFor(search.sort()));
        return PageResponse.from(artworkRepository.findAll(specificationFor(search), pageRequest)
                .map(ArtworkResponse::from));
    }

    @Transactional(readOnly = true)
    public ArtworkResponse get(Long id) {
        return ArtworkResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public List<ArtworkResponse> byArtist(Long artistId) {
        return artworkRepository.findByArtistIdOrderByCreatedAtDesc(artistId).stream()
                .map(ArtworkResponse::from).toList();
    }

    @Transactional
    public ArtworkResponse create(User artist, ArtworkForm form, MultipartFile image) {
        if (!artist.canSell()) {
            throw ApiException.forbidden("Only artist accounts can upload artwork. Switch to an artist profile first.");
        }
        SaleType saleType = form.saleType() == null ? SaleType.FIXED : form.saleType();
        if (saleType == SaleType.AUCTION && form.auctionDurationHours() == null) {
            throw ApiException.badRequest("Choose how long the auction should run");
        }

        Artwork artwork = new Artwork();
        artwork.setArtist(artist);
        artwork.setSaleType(saleType);
        applyForm(artwork, form);
        if (saleType == SaleType.AUCTION) {
            artwork.setAuctionEndsAt(Instant.now().plus(Duration.ofHours(form.auctionDurationHours())));
        }
        artwork.setImageUrl(cloudinaryService.uploadImage(image, "artconnect/artworks"));
        return ArtworkResponse.from(artworkRepository.save(artwork));
    }

    @Transactional
    public ArtworkResponse update(User user, Long id, ArtworkForm form) {
        Artwork artwork = artworkRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("Artwork not found"));
        requireOwnerOrAdmin(user, artwork);
        if (artwork.getStatus() == ArtworkStatus.SOLD) {
            throw ApiException.conflict("Sold artworks can no longer be edited");
        }
        if (artwork.getStatus() == ArtworkStatus.RESERVED) {
            throw ApiException.conflict("This artwork is reserved while a buyer pays and can't be edited right now");
        }
        if (artwork.getBidCount() > 0 && form.price().compareTo(artwork.getPrice()) != 0) {
            throw ApiException.conflict("The starting bid can't change once bidding has started");
        }
        applyForm(artwork, form);
        return ArtworkResponse.from(artwork);
    }

    @Transactional
    public void delete(User user, Long id) {
        Artwork artwork = find(id);
        requireOwnerOrAdmin(user, artwork);
        if (artwork.isAuctionLive(Instant.now()) && artwork.getBidCount() > 0) {
            throw ApiException.conflict("An auction with bids can't be deleted before it ends");
        }
        if (artwork.getStatus() == ArtworkStatus.RESERVED) {
            throw ApiException.conflict("This artwork is reserved while a buyer pays and can't be deleted right now");
        }
        // Remove it from exhibitions through JPA so the ordered join table is rewritten without gaps
        for (Exhibition exhibition : exhibitionRepository.findByArtworksId(id)) {
            exhibition.getArtworks().removeIf(a -> a.getId().equals(id));
        }
        exhibitionRepository.flush();
        artworkRepository.delete(artwork);
    }

    Artwork find(Long id) {
        return artworkRepository.findById(id).orElseThrow(() -> ApiException.notFound("Artwork not found"));
    }

    private static void requireOwnerOrAdmin(User user, Artwork artwork) {
        if (!artwork.isOwnedBy(user) && !user.isAdmin()) {
            throw ApiException.forbidden("You can only change your own artworks");
        }
    }

    private static void applyForm(Artwork artwork, ArtworkForm form) {
        if (!Categories.isValid(form.category())) {
            throw ApiException.badRequest("Choose one of the listed categories");
        }
        artwork.setTitle(form.title().trim());
        artwork.setDescription(form.description().trim());
        artwork.setPrice(form.price());
        artwork.setCategory(form.category());
        artwork.setTags(parseTags(form.tags()));
        artwork.setMedium(blankToNull(form.medium()));
        artwork.setDimensions(blankToNull(form.dimensions()));
        artwork.setYear(form.year());
    }

    static List<String> parseTags(String tags) {
        if (tags == null) return List.of();
        List<String> parsed = Arrays.stream(tags.split(","))
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .toList();
        if (parsed.size() > MAX_TAGS) throw ApiException.badRequest("Use at most " + MAX_TAGS + " tags");
        if (parsed.stream().anyMatch(tag -> tag.length() > MAX_TAG_LENGTH)) {
            throw ApiException.badRequest("Each tag must be at most " + MAX_TAG_LENGTH + " characters");
        }
        return parsed;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Sort sortFor(String sort) {
        Sort order = switch (sort == null ? "" : sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "ending_soon" -> Sort.by(Sort.Direction.ASC, "auctionEndsAt");
            case "top_rated" -> Sort.by(Sort.Direction.DESC, "averageRating", "reviewCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        return order.and(Sort.by(Sort.Direction.DESC, "id"));
    }

    private static Specification<Artwork> specificationFor(ArtworkSearch s) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(s.category())) predicates.add(cb.equal(root.get("category"), s.category()));
            if (s.saleType() != null) predicates.add(cb.equal(root.get("saleType"), s.saleType()));
            if (s.status() != null) predicates.add(cb.equal(root.get("status"), s.status()));
            if (s.artistId() != null) predicates.add(cb.equal(root.get("artist").get("id"), s.artistId()));
            if (s.minPrice() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), s.minPrice()));
            if (s.maxPrice() != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), s.maxPrice()));
            if (hasText(s.tag())) {
                predicates.add(cb.isMember(s.tag().trim().toLowerCase(Locale.ROOT), root.<List<String>>get("tags")));
            }
            if (Boolean.TRUE.equals(s.liveAuctions())) {
                predicates.add(cb.equal(root.get("saleType"), SaleType.AUCTION));
                predicates.add(cb.equal(root.get("status"), ArtworkStatus.AVAILABLE));
                predicates.add(cb.greaterThan(root.get("auctionEndsAt"), Instant.now()));
            }
            if (hasText(s.q())) {
                String pattern = "%" + escapeLike(s.q().trim().toLowerCase(Locale.ROOT)) + "%";
                Join<Artwork, User> artist = root.join("artist");
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description")), pattern, '\\'),
                        cb.like(cb.lower(root.get("category")), pattern, '\\'),
                        cb.like(cb.lower(artist.get("displayName")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
