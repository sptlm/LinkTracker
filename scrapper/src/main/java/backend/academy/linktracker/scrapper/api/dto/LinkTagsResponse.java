package backend.academy.linktracker.scrapper.api.dto;

import java.net.URI;
import java.util.List;

public record LinkTagsResponse(long linkId, URI url, List<String> tags) {}
