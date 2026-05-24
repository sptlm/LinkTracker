package backend.academy.linktracker.scrapper.updater;

import java.time.Instant;

public record LinkUpdateCheckResult(boolean changed, String description, Instant newUpdatedAt, String author) {

    public static LinkUpdateCheckResult unchanged(String description, Instant newUpdatedAt) {
        return new LinkUpdateCheckResult(false, description, newUpdatedAt, null);
    }

    public static LinkUpdateCheckResult changed(String description, Instant newUpdatedAt) {
        return changed(description, newUpdatedAt, null);
    }

    public static LinkUpdateCheckResult changed(String description, Instant newUpdatedAt, String author) {
        return new LinkUpdateCheckResult(true, description, newUpdatedAt, author);
    }
}
