package com.example.ArtConnect.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "exhibitions")
public class Exhibition {
    public enum Status { UPCOMING, ONGOING, PAST }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curator_id")
    private User curator;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @ManyToMany
    @JoinTable(name = "exhibition_artworks",
            joinColumns = @JoinColumn(name = "exhibition_id"),
            inverseJoinColumns = @JoinColumn(name = "artwork_id"))
    @OrderColumn(name = "position")
    private List<Artwork> artworks = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Status statusOn(LocalDate today) {
        if (today.isBefore(startDate)) return Status.UPCOMING;
        if (today.isAfter(endDate)) return Status.PAST;
        return Status.ONGOING;
    }

    public Long getId() { return id; }

    public User getCurator() { return curator; }
    public void setCurator(User curator) { this.curator = curator; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public List<Artwork> getArtworks() { return artworks; }
    public void setArtworks(List<Artwork> artworks) {
        this.artworks.clear();
        this.artworks.addAll(artworks);
    }

    public Instant getCreatedAt() { return createdAt; }
}
