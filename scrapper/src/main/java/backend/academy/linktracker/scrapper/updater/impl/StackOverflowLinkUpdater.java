package backend.academy.linktracker.scrapper.updater.impl;

import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowQuestionItem;
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
public class StackOverflowLinkUpdater implements LinkUpdater {

    private final StackOverflowClient stackOverflowClient;

    @Override
    public boolean supports(TrackedLink link) {
        return link.type() == LinkSourceType.STACKOVERFLOW;
    }

    @Override
    public LinkUpdateCheckResult check(TrackedLink link) {
        long questionId = extractQuestionId(link.url());
        StackOverflowQuestionItem question = stackOverflowClient.getQuestion(questionId);

        Instant observedUpdatedAt = question.lastActivityAt();

        if (link.lastUpdatedAt() == null) {
            return LinkUpdateCheckResult.unchanged(null, observedUpdatedAt);
        }

        if (observedUpdatedAt.isAfter(link.lastUpdatedAt())) {
            String description = "Stack Overflow question updated: %s".formatted(question.title());
            return LinkUpdateCheckResult.changed(description, observedUpdatedAt);
        }

        return LinkUpdateCheckResult.unchanged(null, observedUpdatedAt);
    }

    private long extractQuestionId(String url) {
        URI uri = URI.create(url);
        String path = uri.getPath();

        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        String[] parts = path.split("/");
        if (parts.length < 2 || !"questions".equals(parts[0])) {
            throw new IllegalArgumentException("Invalid StackOverflow question URL: " + url);
        }

        return Long.parseLong(parts[1]);
    }
}
