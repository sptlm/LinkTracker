package backend.academy.linktracker.scrapper.api.dto;

import java.util.List;

public record ListLinksResponse(List<LinkResponse> links, int size) {}
