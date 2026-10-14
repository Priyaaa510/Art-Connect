package com.example.ArtConnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ArtConnect.model.Exhibition;

public interface ExhibitionRepository extends JpaRepository<Exhibition, Long> {
    List<Exhibition> findAllByOrderByStartDateDesc();

    List<Exhibition> findByCuratorIdOrderByStartDateDesc(Long curatorId);

    List<Exhibition> findByArtworksId(Long artworkId);
}
