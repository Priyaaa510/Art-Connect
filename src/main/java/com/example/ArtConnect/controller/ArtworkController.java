package com.example.ArtConnect.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.ArtConnect.dto.ArtworkDtos.ArtworkForm;
import com.example.ArtConnect.dto.ArtworkDtos.ArtworkResponse;
import com.example.ArtConnect.dto.ArtworkDtos.ArtworkSearch;
import com.example.ArtConnect.dto.ArtworkDtos.PageResponse;
import com.example.ArtConnect.dto.MarketDtos.BidRequest;
import com.example.ArtConnect.dto.MarketDtos.BidResponse;
import com.example.ArtConnect.dto.MarketDtos.ReviewRequest;
import com.example.ArtConnect.dto.MarketDtos.ReviewResponse;
import com.example.ArtConnect.model.Categories;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.security.CurrentUser;
import com.example.ArtConnect.service.ArtworkService;
import com.example.ArtConnect.service.AuctionService;
import com.example.ArtConnect.service.ReviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ArtworkController {

    private final ArtworkService artworkService;
    private final AuctionService auctionService;
    private final ReviewService reviewService;

    public ArtworkController(ArtworkService artworkService, AuctionService auctionService,
                             ReviewService reviewService) {
        this.artworkService = artworkService;
        this.auctionService = auctionService;
        this.reviewService = reviewService;
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return Categories.ALL;
    }

    /** Browse with optional filters: q, category, tag, saleType, status, artistId, minPrice, maxPrice, liveAuctions, sort. */
    @GetMapping("/artworks")
    public PageResponse<ArtworkResponse> artworks(@ModelAttribute ArtworkSearch search,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "24") int size) {
        return artworkService.search(search, page, size);
    }

    @GetMapping("/artworks/{id}")
    public ArtworkResponse artwork(@PathVariable Long id) {
        return artworkService.get(id);
    }

    @PostMapping(value = "/artworks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArtworkResponse upload(@CurrentUser User user, @Valid @ModelAttribute ArtworkForm form,
                                  @RequestPart("image") MultipartFile image) {
        return artworkService.create(user, form, image);
    }

    @PutMapping("/artworks/{id}")
    public ArtworkResponse update(@CurrentUser User user, @PathVariable Long id, @Valid @RequestBody ArtworkForm form) {
        return artworkService.update(user, id, form);
    }

    @DeleteMapping("/artworks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser User user, @PathVariable Long id) {
        artworkService.delete(user, id);
    }

    @GetMapping("/artworks/{id}/bids")
    public List<BidResponse> bids(@PathVariable Long id) {
        return auctionService.history(id);
    }

    @PostMapping("/artworks/{id}/bids")
    @ResponseStatus(HttpStatus.CREATED)
    public ArtworkResponse bid(@CurrentUser User user, @PathVariable Long id, @Valid @RequestBody BidRequest request) {
        return auctionService.placeBid(user, id, request.amount());
    }

    @GetMapping("/artworks/{id}/reviews")
    public List<ReviewResponse> reviews(@PathVariable Long id) {
        return reviewService.forArtwork(id);
    }

    @PostMapping("/artworks/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse review(@CurrentUser User user, @PathVariable Long id,
                                 @Valid @RequestBody ReviewRequest request) {
        return reviewService.create(user, id, request);
    }

    @DeleteMapping("/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@CurrentUser User user, @PathVariable Long reviewId) {
        reviewService.delete(user, reviewId);
    }
}
