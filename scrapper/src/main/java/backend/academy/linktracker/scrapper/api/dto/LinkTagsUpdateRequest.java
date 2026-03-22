package backend.academy.linktracker.scrapper.api.dto;

import java.net.URI;
import java.util.List;

public record LinkTagsUpdateRequest(URI link, List<String> tags) {}
