package backend.academy.linktracker.scrapper.updater;

import java.time.Instant;

public record LinkUpdateCheckResult(
        boolean updated,
        String description,
        Instant newUpdatedAt
) {

    public static LinkUpdateCheckResult notChanged() {
        return new LinkUpdateCheckResult(false, null, null);
    }

    public static LinkUpdateCheckResult updated(String description, Instant newUpdatedAt) {
        return new LinkUpdateCheckResult(true, description, newUpdatedAt);
    }
}
