package com.example.ArtConnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CheckoutServiceTest {

    @Test
    void convertsRupeesToPaise() {
        assertThat(CheckoutService.toSmallestUnit(new BigDecimal("4200.00"))).isEqualTo(420000);
        assertThat(CheckoutService.toSmallestUnit(new BigDecimal("1499.99"))).isEqualTo(149999);
    }
}
