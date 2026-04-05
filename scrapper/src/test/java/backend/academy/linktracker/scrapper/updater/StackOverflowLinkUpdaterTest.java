package backend.academy.linktracker.scrapper.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowAnswerItem;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowCommentItem;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowOwner;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowQuestionItem;
import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.updater.impl.StackOverflowLinkUpdater;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StackOverflowLinkUpdaterTest {

    @Mock
    private StackOverflowClient stackOverflowClient;

    @InjectMocks
    private StackOverflowLinkUpdater stackOverflowLinkUpdater;

    @Test
    void supports_returnsTrueOnlyForStackOverflowLinks() {
        TrackedLink stackLink = new TrackedLink(
                1L, "https://stackoverflow.com/questions/123", LinkSourceType.STACKOVERFLOW, Instant.now(), null, null);
        TrackedLink githubLink =
                new TrackedLink(2L, "https://github.com/user/repo", LinkSourceType.GITHUB, Instant.now(), null, null);

        assertTrue(stackOverflowLinkUpdater.supports(stackLink));
        assertFalse(stackOverflowLinkUpdater.supports(githubLink));
    }

    @Test
    void check_whenNoStoredLastUpdatedAt_returnsUnchangedWithObservedTimestamp() {
        TrackedLink link = new TrackedLink(
                1L,
                "https://stackoverflow.com/questions/123/title",
                LinkSourceType.STACKOVERFLOW,
                Instant.now(),
                null,
                null);
        StackOverflowQuestionItem question = org.mockito.Mockito.mock(StackOverflowQuestionItem.class);

        when(stackOverflowClient.getQuestion(123L)).thenReturn(question);
        when(stackOverflowClient.getLatestAnswers(123L, 20)).thenReturn(java.util.List.of());
        when(stackOverflowClient.getLatestComments(123L, 20)).thenReturn(java.util.List.of());
        when(question.lastActivityAt()).thenReturn(Instant.parse("2026-03-08T12:00:00Z"));

        LinkUpdateCheckResult result = stackOverflowLinkUpdater.check(link);

        assertFalse(result.changed());
        assertEquals(Instant.parse("2026-03-08T12:00:00Z"), result.newUpdatedAt());
    }

    @Test
    void check_whenObservedUpdatedAtIsAfterLastUpdated_returnsChanged() {
        TrackedLink link = new TrackedLink(
                1L,
                "https://stackoverflow.com/questions/123/title/",
                LinkSourceType.STACKOVERFLOW,
                Instant.now(),
                null,
                Instant.parse("2026-03-05T10:00:00Z"));
        StackOverflowQuestionItem question = org.mockito.Mockito.mock(StackOverflowQuestionItem.class);

        when(stackOverflowClient.getQuestion(123L)).thenReturn(question);
        when(stackOverflowClient.getLatestAnswers(123L, 20))
                .thenReturn(java.util.List.of(new StackOverflowAnswerItem(
                        1L,
                        Instant.parse("2026-03-08T12:00:00Z").getEpochSecond(),
                        "Body",
                        "Body markdown",
                        new StackOverflowOwner("Bob"))));
        when(stackOverflowClient.getLatestComments(123L, 20)).thenReturn(java.util.List.of());
        when(question.lastActivityAt()).thenReturn(Instant.parse("2026-03-07T12:00:00Z"));
        when(question.title()).thenReturn("How to write tests?");

        LinkUpdateCheckResult result = stackOverflowLinkUpdater.check(link);

        assertTrue(result.changed());
        org.junit.jupiter.api.Assertions.assertTrue(result.description().contains("Тема вопроса: How to write tests?"));
        org.junit.jupiter.api.Assertions.assertTrue(result.description().contains("Время создания: 08.03.2026 12:00:00 UTC"));
        assertEquals(Instant.parse("2026-03-08T12:00:00Z"), result.newUpdatedAt());
    }

    @Test
    void check_whenObservedUpdatedAtIsNotAfterLastUpdated_returnsUnchanged() {
        TrackedLink link = new TrackedLink(
                1L,
                "https://stackoverflow.com/questions/123/title",
                LinkSourceType.STACKOVERFLOW,
                Instant.now(),
                null,
                Instant.parse("2026-03-10T10:00:00Z"));
        StackOverflowQuestionItem question = org.mockito.Mockito.mock(StackOverflowQuestionItem.class);

        when(stackOverflowClient.getQuestion(123L)).thenReturn(question);
        when(stackOverflowClient.getLatestAnswers(123L, 20)).thenReturn(java.util.List.of());
        when(stackOverflowClient.getLatestComments(123L, 20))
                .thenReturn(java.util.List.of(new StackOverflowCommentItem(
                        1L,
                        Instant.parse("2026-03-08T12:00:00Z").getEpochSecond(),
                        "Comment body",
                        "Comment markdown",
                        new StackOverflowOwner("Bob"))));
        when(question.lastActivityAt()).thenReturn(Instant.parse("2026-03-08T12:00:00Z"));

        LinkUpdateCheckResult result = stackOverflowLinkUpdater.check(link);

        assertFalse(result.changed());
        assertEquals(Instant.parse("2026-03-08T12:00:00Z"), result.newUpdatedAt());
    }

    @Test
    void check_whenUrlInvalid_throwsIllegalArgumentException() {
        TrackedLink link = new TrackedLink(
                1L,
                "https://stackoverflow.com/answers/123",
                LinkSourceType.STACKOVERFLOW,
                Instant.now(),
                null,
                Instant.parse("2026-03-05T10:00:00Z"));

        assertThrows(IllegalArgumentException.class, () -> stackOverflowLinkUpdater.check(link));
    }

    @Test
    void check_whenCommentMarkdownMissing_usesBodyAsPreview() {
        TrackedLink link = new TrackedLink(
                1L,
                "https://stackoverflow.com/questions/123/title",
                LinkSourceType.STACKOVERFLOW,
                Instant.now(),
                null,
                Instant.parse("2026-03-05T10:00:00Z"));
        StackOverflowQuestionItem question = org.mockito.Mockito.mock(StackOverflowQuestionItem.class);

        when(stackOverflowClient.getQuestion(123L)).thenReturn(question);
        when(stackOverflowClient.getLatestAnswers(123L, 20)).thenReturn(java.util.List.of());
        when(stackOverflowClient.getLatestComments(123L, 20))
                .thenReturn(java.util.List.of(new StackOverflowCommentItem(
                        2L,
                        Instant.parse("2026-03-08T12:00:00Z").getEpochSecond(),
                        "Body fallback",
                        null,
                        new StackOverflowOwner("Bob"))));
        when(question.lastActivityAt()).thenReturn(Instant.parse("2026-03-07T12:00:00Z"));
        when(question.title()).thenReturn("How to write tests?");

        LinkUpdateCheckResult result = stackOverflowLinkUpdater.check(link);

        assertTrue(result.changed());
        org.junit.jupiter.api.Assertions.assertTrue(result.description().contains("Превью: Body fallback"));
    }
}
