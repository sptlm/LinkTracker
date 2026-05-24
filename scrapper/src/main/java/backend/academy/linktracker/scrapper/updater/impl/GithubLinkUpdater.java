package backend.academy.linktracker.scrapper.updater.impl;

import backend.academy.linktracker.scrapper.client.github.GithubClient;
import backend.academy.linktracker.scrapper.client.github.dto.GithubIssueItem;
import backend.academy.linktracker.scrapper.client.github.dto.GithubRepositoryResponse;
import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.updater.LinkUpdateCheckResult;
import backend.academy.linktracker.scrapper.updater.LinkUpdater;
import backend.academy.linktracker.scrapper.updater.MessageFormattingUtils;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GithubLinkUpdater implements LinkUpdater {

    private static final int PREVIEW_LENGTH = 200;
    private static final int EVENTS_FETCH_LIMIT = 20;

    private final GithubClient githubClient;

    @Override
    public boolean supports(TrackedLink link) {
        return link.type() == LinkSourceType.GITHUB;
    }

    @Override
    public LinkUpdateCheckResult check(TrackedLink link) {
        RepoCoordinates coordinates = extractCoordinates(link.url());
        List<GithubIssueItem> latestEvents = githubClient.getLatestIssuesAndPullRequests(
                coordinates.owner(), coordinates.repo(), EVENTS_FETCH_LIMIT);

        Instant observedUpdatedAt = latestEvents.stream()
                .map(GithubIssueItem::createdAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseGet(() -> {
                    GithubRepositoryResponse response =
                            githubClient.getRepository(coordinates.owner(), coordinates.repo());
                    return response.pushedAt() != null ? response.pushedAt() : response.updatedAt();
                });

        if (observedUpdatedAt == null) {
            return LinkUpdateCheckResult.unchanged(null, link.lastUpdatedAt());
        }

        if (link.lastUpdatedAt() == null) {
            return LinkUpdateCheckResult.unchanged(null, observedUpdatedAt);
        }

        GithubIssueItem newEvent = latestEvents.stream()
                .filter(item -> item.createdAt() != null)
                .filter(item -> item.createdAt().isAfter(link.lastUpdatedAt()))
                .max(Comparator.comparing(GithubIssueItem::createdAt))
                .orElse(null);

        if (newEvent != null) {
            String author = newEvent.user() != null ? newEvent.user().login() : null;
            return LinkUpdateCheckResult.changed(formatDescription(newEvent), observedUpdatedAt, author);
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

    private String formatDescription(GithubIssueItem item) {
        String type = item.isPullRequest() ? "PR" : "Issue";
        String title = MessageFormattingUtils.safe(item.title());
        String author =
                item.user() != null ? MessageFormattingUtils.safe(item.user().login()) : "unknown";
        String createdAt = MessageFormattingUtils.formatInstant(item.createdAt());
        String preview =
                MessageFormattingUtils.truncateWithEllipsis(MessageFormattingUtils.safe(item.body()), PREVIEW_LENGTH);
        return "GitHub %s обновление%nНазвание: %s%nПользователь: %s%nВремя создания: %s%nПревью: %s"
                .formatted(type, title, author, createdAt, preview);
    }
}
