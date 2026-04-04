package backend.academy.linktracker.scrapper.api.dto;

import java.net.URI;

public record LinkTagOperationRequest(URI link, String tag) {}
