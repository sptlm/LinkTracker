package backend.academy.linktracker.scrapper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdateRequest;
import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.updater.LinkUpdateCheckResult;
import backend.academy.linktracker.scrapper.updater.LinkUpdater;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkPollingServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private LinkUpdater githubUpdater;

    @Mock
    private LinkUpdater stackOverflowUpdater;

    @Mock
    private UpdatePublisher updatePublisher;

    private LinkPollingService linkPollingService;

    @BeforeEach
    void setUp() {
        linkPollingService = new LinkPollingService(
                linkRepository, subscriptionRepository, List.of(githubUpdater, stackOverflowUpdater), updatePublisher);
    }

    @Test
    void pollUpdates_whenChangedWithoutUpdatedAt_usesCheckedAtAndPublishes() {
        TrackedLink link = new TrackedLink(
                1L,
                "https://github.com/user/repo",
                LinkSourceType.GITHUB,
                Instant.parse("2026-03-01T10:00:00Z"),
                null,
                Instant.parse("2026-03-05T10:00:00Z"));

        when(linkRepository.findAll()).thenReturn(List.of(link));
        when(githubUpdater.supports(link)).thenReturn(true);
        when(githubUpdater.check(link)).thenReturn(LinkUpdateCheckResult.changed("repo updated", null));
        when(subscriptionRepository.findChatIdsByLinkId(1L)).thenReturn(List.of(101L, 202L));

        linkPollingService.pollUpdates();

        ArgumentCaptor<Instant> checkedAtCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> updatedAtCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(linkRepository)
                .updatePollingState(org.mockito.Mockito.eq(1L), checkedAtCaptor.capture(), updatedAtCaptor.capture());

        assertEquals(checkedAtCaptor.getValue(), updatedAtCaptor.getValue());

        ArgumentCaptor<LinkUpdateRequest> requestCaptor = ArgumentCaptor.forClass(LinkUpdateRequest.class);
        verify(updatePublisher).publish(requestCaptor.capture());

        LinkUpdateRequest request = requestCaptor.getValue();
        assertEquals(1L, request.id());
        assertEquals("https://github.com/user/repo", request.url());
        assertEquals("repo updated", request.description());
        assertEquals(List.of(101L, 202L), request.tgChatIds());
    }

    @Test
    void pollUpdates_whenChangedWithSubscribers_publishesUpdate() {
        Instant newUpdatedAt = Instant.parse("2026-03-08T12:00:00Z");

        TrackedLink link = new TrackedLink(
                1L,
                "https://stackoverflow.com/questions/123",
                LinkSourceType.STACKOVERFLOW,
                Instant.parse("2026-03-01T10:00:00Z"),
                null,
                Instant.parse("2026-03-05T10:00:00Z"));

        when(linkRepository.findAll()).thenReturn(List.of(link));
        when(githubUpdater.supports(link)).thenReturn(false);
        when(stackOverflowUpdater.supports(link)).thenReturn(true);
        when(stackOverflowUpdater.check(link))
                .thenReturn(LinkUpdateCheckResult.changed("question updated", newUpdatedAt));
        when(subscriptionRepository.findChatIdsByLinkId(1L)).thenReturn(List.of(777L));

        linkPollingService.pollUpdates();

        verify(linkRepository)
                .updatePollingState(
                        org.mockito.Mockito.eq(1L), any(Instant.class), org.mockito.Mockito.eq(newUpdatedAt));

        ArgumentCaptor<LinkUpdateRequest> requestCaptor = ArgumentCaptor.forClass(LinkUpdateRequest.class);
        verify(updatePublisher).publish(requestCaptor.capture());

        LinkUpdateRequest request = requestCaptor.getValue();
        assertEquals(1L, request.id());
        assertEquals("https://stackoverflow.com/questions/123", request.url());
        assertEquals("question updated", request.description());
        assertEquals(List.of(777L), request.tgChatIds());
    }

    @Test
    void pollUpdates_whenNoUpdaterFound_throwsIllegalStateException() {
        TrackedLink link = new TrackedLink(
                1L,
                "https://example.com/resource",
                LinkSourceType.GITHUB,
                Instant.parse("2026-03-01T10:00:00Z"),
                null,
                Instant.parse("2026-03-05T10:00:00Z"));

        when(linkRepository.findAll()).thenReturn(List.of(link));
        when(githubUpdater.supports(link)).thenReturn(false);
        when(stackOverflowUpdater.supports(link)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> linkPollingService.pollUpdates());
    }
}
