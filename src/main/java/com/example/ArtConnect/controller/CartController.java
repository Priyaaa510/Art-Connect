package com.example.ArtConnect.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ArtConnect.dto.MarketDtos.AddToCartRequest;
import com.example.ArtConnect.dto.MarketDtos.CartResponse;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.security.CurrentUser;
import com.example.ArtConnect.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse cart(@CurrentUser User user) {
        return cartService.cartOf(user);
    }

    @PostMapping("/items")
    public CartResponse add(@CurrentUser User user, @Valid @RequestBody AddToCartRequest request) {
        return cartService.add(user, request.artworkId());
    }

    @DeleteMapping("/items/{artworkId}")
    public CartResponse remove(@CurrentUser User user, @PathVariable Long artworkId) {
        return cartService.remove(user, artworkId);
    }
}
