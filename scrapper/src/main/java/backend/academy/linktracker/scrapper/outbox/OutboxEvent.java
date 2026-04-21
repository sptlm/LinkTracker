package backend.academy.linktracker.scrapper.outbox;

import java.time.OffsetDateTime;

public record OutboxEvent(long id, String payload, int attempts, OffsetDateTime createdAt) {}
