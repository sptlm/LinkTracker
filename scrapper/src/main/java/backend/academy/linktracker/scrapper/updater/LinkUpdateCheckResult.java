package backend.academy.linktracker.scrapper.updater;

import java.time.Instant;

public record LinkUpdateCheckResult(boolean changed, String description, Instant newUpdatedAt) {

    public static LinkUpdateCheckResult unchanged(String description, Instant newUpdatedAt) {
        return new LinkUpdateCheckResult(false, description, newUpdatedAt);
    }

    public static LinkUpdateCheckResult changed(String description, Instant newUpdatedAt) {
        return new LinkUpdateCheckResult(true, description, newUpdatedAt);
    }
}
