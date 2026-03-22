package backend.academy.linktracker.scrapper.model;

import java.time.Instant;
import java.util.List;

public record LinkSubscription(long chatId, long linkId, List<String> tags, Instant createdAt) {}
