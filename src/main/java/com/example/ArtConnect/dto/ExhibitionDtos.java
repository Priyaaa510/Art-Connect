package com.example.ArtConnect.dto;

import java.time.LocalDate;
import java.util.List;

import com.example.ArtConnect.dto.ArtworkDtos.ArtworkResponse;
import com.example.ArtConnect.model.Exhibition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ExhibitionDtos {
    private ExhibitionDtos() {}

    public record ExhibitionRequest(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 2000) String description,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotEmpty(message = "must include at least one artwork") @Size(max = 50) List<Long> artworkIds) {}

    public record ExhibitionSummary(Long id, String title, String description, Long curatorId, String curatorName,
                                    LocalDate startDate, LocalDate endDate, Exhibition.Status status,
                                    String coverImageUrl, int artworkCount) {
        public static ExhibitionSummary from(Exhibition e) {
            String cover = e.getArtworks().isEmpty() ? null : e.getArtworks().get(0).getImageUrl();
            return new ExhibitionSummary(e.getId(), e.getTitle(), e.getDescription(), e.getCurator().getId(),
                    e.getCurator().getDisplayName(), e.getStartDate(), e.getEndDate(),
                    e.statusOn(LocalDate.now()), cover, e.getArtworks().size());
        }
    }

    public record ExhibitionDetails(ExhibitionSummary exhibition, List<ArtworkResponse> artworks) {
        public static ExhibitionDetails from(Exhibition e) {
            return new ExhibitionDetails(ExhibitionSummary.from(e),
                    e.getArtworks().stream().map(ArtworkResponse::from).toList());
        }
    }
}
