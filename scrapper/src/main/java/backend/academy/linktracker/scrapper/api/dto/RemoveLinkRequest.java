package backend.academy.linktracker.scrapper.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RemoveLinkRequest(@NotBlank String link) {}
