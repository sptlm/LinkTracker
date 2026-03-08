package backend.academy.linktracker.scrapper.model;

import java.time.Instant;

public record TrackedLink(
        long id, String url, LinkSourceType type, Instant createdAt, Instant lastCheckedAt, Instant lastUpdatedAt) {

    public TrackedLink withLastCheckedAt(Instant lastCheckedAt) {
        return new TrackedLink(id, url, type, createdAt, lastCheckedAt, lastUpdatedAt);
    }

    public TrackedLink withLastUpdatedAt(Instant lastUpdatedAt) {
        return new TrackedLink(id, url, type, createdAt, lastCheckedAt, lastUpdatedAt);
    }

    public TrackedLink withPollingState(Instant lastCheckedAt, Instant lastUpdatedAt) {
        return new TrackedLink(id, url, type, createdAt, lastCheckedAt, lastUpdatedAt);
    }
}
