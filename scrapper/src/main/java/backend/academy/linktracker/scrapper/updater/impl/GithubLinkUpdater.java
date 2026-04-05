package backend.academy.linktracker.scrapper.updater.impl;

import backend.academy.linktracker.scrapper.client.github.GithubClient;
import backend.academy.linktracker.scrapper.client.github.dto.GithubIssueItem;
import backend.academy.linktracker.scrapper.client.github.dto.GithubRepositoryResponse;
import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.updater.LinkUpdateCheckResult;
import backend.academy.linktracker.scrapper.updater.LinkUpdater;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GithubLinkUpdater implements LinkUpdater {

    private static final int PREVIEW_LENGTH = 200;
    private static final int EVENTS_FETCH_LIMIT = 20;
    private static final DateTimeFormatter MESSAGE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private final GithubClient githubClient;

    @Override
    public boolean supports(TrackedLink link) {
        return link.type() == LinkSourceType.GITHUB;
    }

    @Override
    public LinkUpdateCheckResult check(TrackedLink link) {
        RepoCoordinates coordinates = extractCoordinates(link.url());
        GithubRepositoryResponse response = githubClient.getRepository(coordinates.owner(), coordinates.repo());
        List<GithubIssueItem> latestEvents = githubClient.getLatestIssuesAndPullRequests(
                coordinates.owner(), coordinates.repo(), EVENTS_FETCH_LIMIT);

        Instant observedUpdatedAt = latestEvents.stream()
                .map(GithubIssueItem::createdAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(response.pushedAt() != null ? response.pushedAt() : response.updatedAt());

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
            return LinkUpdateCheckResult.changed(formatDescription(newEvent), observedUpdatedAt);
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
        String title = safe(item.title());
        String author = item.user() != null ? safe(item.user().login()) : "unknown";
        String createdAt = formatInstant(item.createdAt());
        String preview = truncate(safe(item.body()));
        return """
                GitHub %s обновление
                Название: %s
                Пользователь: %s
                Время создания: %s
                Превью: %s
                """.formatted(type, title, author, createdAt, preview);
    }

    private String truncate(String value) {
        return value.length() <= PREVIEW_LENGTH ? value : value.substring(0, PREVIEW_LENGTH);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatInstant(Instant value) {
        return value == null ? "unknown" : MESSAGE_TIME_FORMATTER.format(value);
    }
}
