package backend.academy.linktracker.scrapper.parser;

import backend.academy.linktracker.scrapper.model.LinkSourceType;

public record ParsedLink(String normalizedUrl, LinkSourceType type) {}
