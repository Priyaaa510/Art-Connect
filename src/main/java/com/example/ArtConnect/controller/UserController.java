package com.example.ArtConnect.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.ArtConnect.dto.ArtworkDtos.ArtworkResponse;
import com.example.ArtConnect.dto.MarketDtos.MyBidResponse;
import com.example.ArtConnect.dto.MarketDtos.OrderResponse;
import com.example.ArtConnect.dto.UserDtos.ArtistProfile;
import com.example.ArtConnect.dto.UserDtos.ArtistSummary;
import com.example.ArtConnect.dto.UserDtos.ProfileUpdateRequest;
import com.example.ArtConnect.dto.UserDtos.UserProfile;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.security.CurrentUser;
import com.example.ArtConnect.service.ArtworkService;
import com.example.ArtConnect.service.AuctionService;
import com.example.ArtConnect.service.OrderService;
import com.example.ArtConnect.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final ArtworkService artworkService;
    private final OrderService orderService;
    private final AuctionService auctionService;

    public UserController(UserService userService, ArtworkService artworkService,
                          OrderService orderService, AuctionService auctionService) {
        this.userService = userService;
        this.artworkService = artworkService;
        this.orderService = orderService;
        this.auctionService = auctionService;
    }

    @GetMapping("/me")
    public UserProfile me(@CurrentUser User user) {
        return UserProfile.from(user);
    }

    @PutMapping("/me")
    public UserProfile updateProfile(@CurrentUser User user, @Valid @RequestBody ProfileUpdateRequest request) {
        return userService.updateProfile(user, request);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfile updateAvatar(@CurrentUser User user, @RequestPart("image") MultipartFile image) {
        return userService.updateAvatar(user, image);
    }

    @GetMapping("/me/artworks")
    public List<ArtworkResponse> myArtworks(@CurrentUser User user) {
        return artworkService.byArtist(user.getId());
    }

    @GetMapping("/me/orders")
    public List<OrderResponse> myOrders(@CurrentUser User user) {
        return orderService.purchasesOf(user);
    }

    @GetMapping("/me/sales")
    public List<OrderResponse> mySales(@CurrentUser User user) {
        return orderService.salesOf(user);
    }

    @GetMapping("/me/bids")
    public List<MyBidResponse> myBids(@CurrentUser User user) {
        return auctionService.bidsOf(user);
    }

    @GetMapping("/artists")
    public List<ArtistSummary> artists() {
        return userService.artists();
    }

    @GetMapping("/artists/{id}")
    public ArtistProfile artist(@PathVariable Long id) {
        return userService.artistProfile(id);
    }
}
