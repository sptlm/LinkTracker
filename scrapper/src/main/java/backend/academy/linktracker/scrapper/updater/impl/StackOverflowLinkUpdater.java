package backend.academy.linktracker.scrapper.updater.impl;

import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowAnswerItem;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowCommentItem;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowQuestionItem;
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
public class StackOverflowLinkUpdater implements LinkUpdater {

    private static final int EVENTS_FETCH_LIMIT = 20;
    private static final int PREVIEW_LENGTH = 200;
    private static final DateTimeFormatter MESSAGE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private final StackOverflowClient stackOverflowClient;

    @Override
    public boolean supports(TrackedLink link) {
        return link.type() == LinkSourceType.STACKOVERFLOW;
    }

    @Override
    public LinkUpdateCheckResult check(TrackedLink link) {
        long questionId = extractQuestionId(link.url());
        StackOverflowQuestionItem question = stackOverflowClient.getQuestion(questionId);
        List<StackOverflowAnswerItem> answers = stackOverflowClient.getLatestAnswers(questionId, EVENTS_FETCH_LIMIT);
        List<StackOverflowCommentItem> comments = stackOverflowClient.getLatestComments(questionId, EVENTS_FETCH_LIMIT);

        Instant observedUpdatedAt = java.util.stream.Stream.concat(
                        answers.stream().map(StackOverflowAnswerItem::createdAt),
                        comments.stream().map(StackOverflowCommentItem::createdAt))
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(question.lastActivityAt());

        if (link.lastUpdatedAt() == null) {
            return LinkUpdateCheckResult.unchanged(null, observedUpdatedAt);
        }

        TrackedEvent newEvent = findNewEvent(link.lastUpdatedAt(), answers, comments);
        if (newEvent != null) {
            String description = formatDescription(question.title(), newEvent);
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

    private TrackedEvent findNewEvent(
            Instant lastUpdatedAt, List<StackOverflowAnswerItem> answers, List<StackOverflowCommentItem> comments) {
        TrackedEvent answerEvent = answers.stream()
                .filter(item -> item.createdAt() != null && item.createdAt().isAfter(lastUpdatedAt))
                .max(Comparator.comparing(StackOverflowAnswerItem::createdAt))
                .map(answer -> new TrackedEvent(
                        "ответ",
                        answer.createdAt(),
                        answer.owner() != null ? answer.owner().displayName() : null,
                        answer.preview()))
                .orElse(null);

        TrackedEvent commentEvent = comments.stream()
                .filter(item -> item.createdAt() != null && item.createdAt().isAfter(lastUpdatedAt))
                .max(Comparator.comparing(StackOverflowCommentItem::createdAt))
                .map(comment -> new TrackedEvent(
                        "комментарий",
                        comment.createdAt(),
                        comment.owner() != null ? comment.owner().displayName() : null,
                        comment.preview()))
                .orElse(null);

        if (answerEvent == null) {
            return commentEvent;
        }
        if (commentEvent == null) {
            return answerEvent;
        }
        return answerEvent.createdAt().isAfter(commentEvent.createdAt()) ? answerEvent : commentEvent;
    }

    private String formatDescription(String questionTitle, TrackedEvent event) {
        String createdAt = formatInstant(event.createdAt());
        return """
                StackOverflow: новый %s
                Тема вопроса: %s
                Пользователь: %s
                Время создания: %s
                Превью: %s
                """.formatted(
                event.type(), safe(questionTitle), safe(event.user()), createdAt, truncate(safe(event.preview())));
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

    private record TrackedEvent(String type, Instant createdAt, String user, String preview) {}
}
