package com.example.ArtConnect.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.ArtConnect.dto.ExhibitionDtos.ExhibitionDetails;
import com.example.ArtConnect.dto.ExhibitionDtos.ExhibitionRequest;
import com.example.ArtConnect.dto.ExhibitionDtos.ExhibitionSummary;
import com.example.ArtConnect.model.Exhibition;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.security.CurrentUser;
import com.example.ArtConnect.service.ExhibitionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/exhibitions")
public class ExhibitionController {

    private final ExhibitionService exhibitionService;

    public ExhibitionController(ExhibitionService exhibitionService) {
        this.exhibitionService = exhibitionService;
    }

    @GetMapping
    public List<ExhibitionSummary> list(@RequestParam(required = false) Exhibition.Status status) {
        return exhibitionService.list(status);
    }

    @GetMapping("/{id}")
    public ExhibitionDetails get(@PathVariable Long id) {
        return exhibitionService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExhibitionDetails create(@CurrentUser User user, @Valid @RequestBody ExhibitionRequest request) {
        return exhibitionService.create(user, request);
    }

    @PutMapping("/{id}")
    public ExhibitionDetails update(@CurrentUser User user, @PathVariable Long id,
                                    @Valid @RequestBody ExhibitionRequest request) {
        return exhibitionService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser User user, @PathVariable Long id) {
        exhibitionService.delete(user, id);
    }
}
