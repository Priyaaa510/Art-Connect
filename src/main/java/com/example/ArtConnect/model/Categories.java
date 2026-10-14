package com.example.ArtConnect.model;

import java.util.List;

public final class Categories {
    public static final List<String> ALL = List.of(
            "Painting", "Drawing", "Photography", "Digital Art", "Sculpture",
            "Printmaking", "Mixed Media", "Textile Art", "Other");

    private Categories() {}

    public static boolean isValid(String category) {
        return ALL.contains(category);
    }
}
