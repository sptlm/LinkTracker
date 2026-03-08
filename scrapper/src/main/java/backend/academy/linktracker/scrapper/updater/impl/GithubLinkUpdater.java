package backend.academy.linktracker.scrapper.updater.impl;

import backend.academy.linktracker.scrapper.client.github.GithubClient;
import backend.academy.linktracker.scrapper.client.github.dto.GithubRepositoryResponse;
import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.net.URI;
import java.time.Instant;
import backend.academy.linktracker.scrapper.updater.LinkUpdateCheckResult;
import backend.academy.linktracker.scrapper.updater.LinkUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GithubLinkUpdater implements LinkUpdater {

    private final GithubClient githubClient;

    @Override
    public boolean supports(TrackedLink link) {
        return link.type() == LinkSourceType.GITHUB;
    }

    @Override
    public LinkUpdateCheckResult check(TrackedLink link) {
        RepoCoordinates coordinates = extractCoordinates(link.url());
        GithubRepositoryResponse response = githubClient.getRepository(coordinates.owner(), coordinates.repo());

        Instant observedUpdatedAt = response.pushedAt() != null
            ? response.pushedAt()
            : response.updatedAt();

        if (observedUpdatedAt == null) {
            return LinkUpdateCheckResult.unchanged(null, link.lastUpdatedAt());
        }

        if (link.lastUpdatedAt() == null) {
            return LinkUpdateCheckResult.unchanged(null, observedUpdatedAt);
        }

        if (observedUpdatedAt.isAfter(link.lastUpdatedAt())) {
            String description = "GitHub repository updated: %s".formatted(response.fullName());
            return LinkUpdateCheckResult.changed(description, observedUpdatedAt);
        }

        return LinkUpdateCheckResult.unchanged(null, observedUpdatedAt);
    }

    private RepoCoordinates extractCoordinates(String url) {
        URI uri = URI.create(url);
        String path = uri.getPath();

        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        String[] parts = path.split("/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub repository URL: " + url);
        }

        return new RepoCoordinates(parts[0], parts[1]);
    }

    private record RepoCoordinates(String owner, String repo) {}
}
