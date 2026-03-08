package backend.academy.linktracker.scrapper.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.github.GithubClient;
import backend.academy.linktracker.scrapper.client.github.dto.GithubRepositoryResponse;
import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.updater.LinkUpdateCheckResult;
import java.time.Instant;
import backend.academy.linktracker.scrapper.updater.impl.GithubLinkUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GithubLinkUpdaterTest {

    @Mock
    private GithubClient githubClient;

    @InjectMocks
    private GithubLinkUpdater githubLinkUpdater;

    @Test
    void supports_returnsTrueOnlyForGithubLinks() {
        TrackedLink githubLink = new TrackedLink(1L, "https://github.com/user/repo", LinkSourceType.GITHUB, Instant.now(), null, null);
        TrackedLink stackLink = new TrackedLink(2L, "https://stackoverflow.com/questions/123", LinkSourceType.STACKOVERFLOW, Instant.now(), null, null);

        assertTrue(githubLinkUpdater.supports(githubLink));
        assertFalse(githubLinkUpdater.supports(stackLink));
    }

    @Test
    void check_whenObservedUpdatedAtIsAfterLastUpdated_returnsChanged() {
        TrackedLink link = new TrackedLink(
            1L,
            "https://github.com/user/repo/",
            LinkSourceType.GITHUB,
            Instant.now(),
            null,
            Instant.parse("2026-03-05T10:00:00Z")
        );
        GithubRepositoryResponse response = org.mockito.Mockito.mock(GithubRepositoryResponse.class);

        when(githubClient.getRepository("user", "repo")).thenReturn(response);
        when(response.pushedAt()).thenReturn(Instant.parse("2026-03-08T12:00:00Z"));
        when(response.fullName()).thenReturn("user/repo");

        LinkUpdateCheckResult result = githubLinkUpdater.check(link);

        assertTrue(result.changed());
        assertEquals("GitHub repository updated: user/repo", result.description());
        assertEquals(Instant.parse("2026-03-08T12:00:00Z"), result.newUpdatedAt());
    }

    @Test
    void check_whenGithubReturnsNoTimestamps_returnsUnchangedWithCurrentLinkTimestamp() {
        TrackedLink link = new TrackedLink(
            1L,
            "https://github.com/user/repo",
            LinkSourceType.GITHUB,
            Instant.now(),
            null,
            Instant.parse("2026-03-05T10:00:00Z")
        );
        GithubRepositoryResponse response = org.mockito.Mockito.mock(GithubRepositoryResponse.class);

        when(githubClient.getRepository("user", "repo")).thenReturn(response);
        when(response.pushedAt()).thenReturn(null);
        when(response.updatedAt()).thenReturn(null);

        LinkUpdateCheckResult result = githubLinkUpdater.check(link);

        assertFalse(result.changed());
        assertEquals(Instant.parse("2026-03-05T10:00:00Z"), result.newUpdatedAt());
    }

    @Test
    void check_whenUrlInvalid_throwsIllegalArgumentException() {
        TrackedLink link = new TrackedLink(
            1L,
            "https://github.com/user",
            LinkSourceType.GITHUB,
            Instant.now(),
            null,
            Instant.parse("2026-03-05T10:00:00Z")
        );

        assertThrows(IllegalArgumentException.class, () -> githubLinkUpdater.check(link));
    }
}
