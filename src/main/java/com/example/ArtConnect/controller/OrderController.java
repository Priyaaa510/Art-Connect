package com.example.ArtConnect.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ArtConnect.dto.MarketDtos.OrderResponse;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.security.CurrentUser;
import com.example.ArtConnect.service.CheckoutService;
import com.example.ArtConnect.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final CheckoutService checkoutService;

    public OrderController(OrderService orderService, CheckoutService checkoutService) {
        this.orderService = orderService;
        this.checkoutService = checkoutService;
    }

    /** Cancels an unpaid order (and the rest of its checkout); reserved artworks go back on sale. */
    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@CurrentUser User user, @PathVariable Long orderId) {
        return checkoutService.cancelOrder(user, orderId);
    }

    @PostMapping("/{orderId}/complete")
    public OrderResponse complete(@CurrentUser User user, @PathVariable Long orderId) {
        return orderService.complete(user, orderId);
    }
}
