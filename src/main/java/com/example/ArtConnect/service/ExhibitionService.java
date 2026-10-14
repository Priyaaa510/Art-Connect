package com.example.ArtConnect.service;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ArtConnect.dto.ExhibitionDtos.ExhibitionDetails;
import com.example.ArtConnect.dto.ExhibitionDtos.ExhibitionRequest;
import com.example.ArtConnect.dto.ExhibitionDtos.ExhibitionSummary;
import com.example.ArtConnect.exception.ApiException;
import com.example.ArtConnect.model.Artwork;
import com.example.ArtConnect.model.Exhibition;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.repository.ArtworkRepository;
import com.example.ArtConnect.repository.ExhibitionRepository;

@Service
public class ExhibitionService {
    private final ExhibitionRepository exhibitionRepository;
    private final ArtworkRepository artworkRepository;

    public ExhibitionService(ExhibitionRepository exhibitionRepository, ArtworkRepository artworkRepository) {
        this.exhibitionRepository = exhibitionRepository;
        this.artworkRepository = artworkRepository;
    }

    @Transactional(readOnly = true)
    public List<ExhibitionSummary> list(Exhibition.Status status) {
        LocalDate today = LocalDate.now();
        return exhibitionRepository.findAllByOrderByStartDateDesc().stream()
                .filter(e -> status == null || e.statusOn(today) == status)
                .map(ExhibitionSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExhibitionSummary> byCurator(Long curatorId) {
        return exhibitionRepository.findByCuratorIdOrderByStartDateDesc(curatorId).stream()
                .map(ExhibitionSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public ExhibitionDetails get(Long id) {
        return ExhibitionDetails.from(find(id));
    }

    @Transactional
    public ExhibitionDetails create(User curator, ExhibitionRequest request) {
        if (!curator.canSell()) {
            throw ApiException.forbidden("Only artists can create exhibitions");
        }
        Exhibition exhibition = new Exhibition();
        exhibition.setCurator(curator);
        apply(exhibition, curator, request);
        return ExhibitionDetails.from(exhibitionRepository.save(exhibition));
    }

    @Transactional
    public ExhibitionDetails update(User user, Long id, ExhibitionRequest request) {
        Exhibition exhibition = find(id);
        requireCuratorOrAdmin(user, exhibition);
        apply(exhibition, user, request);
        return ExhibitionDetails.from(exhibition);
    }

    @Transactional
    public void delete(User user, Long id) {
        Exhibition exhibition = find(id);
        requireCuratorOrAdmin(user, exhibition);
        exhibitionRepository.delete(exhibition);
    }

    private Exhibition find(Long id) {
        return exhibitionRepository.findById(id).orElseThrow(() -> ApiException.notFound("Exhibition not found"));
    }

    private static void requireCuratorOrAdmin(User user, Exhibition exhibition) {
        if (!exhibition.getCurator().sameAs(user) && !user.isAdmin()) {
            throw ApiException.forbidden("You can only change your own exhibitions");
        }
    }

    private void apply(Exhibition exhibition, User editor, ExhibitionRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw ApiException.badRequest("The end date must be on or after the start date");
        }
        List<Long> ids = List.copyOf(new LinkedHashSet<>(request.artworkIds()));
        Map<Long, Artwork> found = artworkRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Artwork::getId, Function.identity()));
        if (found.size() != ids.size()) {
            throw ApiException.badRequest("One or more of the selected artworks no longer exist");
        }
        List<Artwork> artworks = ids.stream().map(found::get).toList();
        if (!editor.isAdmin() && artworks.stream().anyMatch(a -> !a.isOwnedBy(exhibition.getCurator()))) {
            throw ApiException.forbidden("Exhibitions can only include the curator's own artworks");
        }
        exhibition.setTitle(request.title().trim());
        exhibition.setDescription(request.description().trim());
        exhibition.setStartDate(request.startDate());
        exhibition.setEndDate(request.endDate());
        exhibition.setArtworks(artworks);
    }
}
