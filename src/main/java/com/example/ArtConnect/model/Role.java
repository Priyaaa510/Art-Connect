package com.example.ArtConnect.model;

public enum Role {
    /** Can upload and sell artwork and curate exhibitions, as well as buy. */
    ARTIST,
    /** Can browse, buy, bid and review. */
    COLLECTOR,
    /** Can moderate everything. Only assignable directly in the database. */
    ADMIN
}
