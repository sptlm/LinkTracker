package backend.academy.linktracker.scrapper.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.ScrapperPollingProperties;
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
    private LinkUpdater linkUpdater;

    @Mock
    private UpdatePublisher updatePublisher;

    private LinkPollingService linkPollingService;

    @BeforeEach
    void setUp() {
        ScrapperPollingProperties pollingProperties = new ScrapperPollingProperties();
        pollingProperties.setBatchSize(100);
        pollingProperties.setWorkerThreads(2);
        linkPollingService = new LinkPollingService(
                linkRepository, subscriptionRepository, List.of(linkUpdater), updatePublisher, pollingProperties);
    }

    @Test
    void shouldPublishUpdateOnlyForSubscribedUsers() {
        TrackedLink link = new TrackedLink(
                10L,
                "https://github.com/user/repo",
                LinkSourceType.GITHUB,
                Instant.parse("2026-03-20T10:00:00Z"),
                Instant.parse("2026-03-20T10:00:00Z"),
                Instant.parse("2026-03-20T10:00:00Z"));

        when(linkRepository.findPage(0, 100)).thenReturn(List.of(link));
        when(linkRepository.findPage(1, 100)).thenReturn(List.of());
        when(linkUpdater.supports(link)).thenReturn(true);
        when(linkUpdater.check(link))
                .thenReturn(LinkUpdateCheckResult.changed(
                        "GitHub repository updated: user/repo", Instant.parse("2026-03-21T10:00:00Z")));
        when(subscriptionRepository.findChatIdsByLinkId(10L)).thenReturn(List.of(1L, 3L));

        linkPollingService.pollUpdates();

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(updatePublisher).publish(captor.capture());
        verify(linkRepository).updatePollingState(any(Long.class), any(Instant.class), any(Instant.class));
        org.junit.jupiter.api.Assertions.assertEquals(
                List.of(1L, 3L), captor.getValue().getTgChatIds());
    }

    @Test
    void shouldNotPublishUpdateWhenNoSubscribersExist() {
        TrackedLink link = new TrackedLink(
                10L,
                "https://github.com/user/repo",
                LinkSourceType.GITHUB,
                Instant.parse("2026-03-20T10:00:00Z"),
                Instant.parse("2026-03-20T10:00:00Z"),
                Instant.parse("2026-03-20T10:00:00Z"));

        when(linkRepository.findPage(0, 100)).thenReturn(List.of(link));
        when(linkRepository.findPage(1, 100)).thenReturn(List.of());
        when(linkUpdater.supports(link)).thenReturn(true);
        when(linkUpdater.check(link))
                .thenReturn(LinkUpdateCheckResult.changed(
                        "GitHub repository updated: user/repo", Instant.parse("2026-03-21T10:00:00Z")));
        when(subscriptionRepository.findChatIdsByLinkId(10L)).thenReturn(List.of());

        linkPollingService.pollUpdates();

        verify(updatePublisher, never()).publish(any());
    }

    @Test
    void shouldIsolateSingleLinkFailureAndContinueBatch() {
        TrackedLink failedLink = new TrackedLink(
                10L,
                "https://github.com/user/failed",
                LinkSourceType.GITHUB,
                Instant.parse("2026-03-20T10:00:00Z"),
                Instant.parse("2026-03-20T10:00:00Z"),
                Instant.parse("2026-03-20T10:00:00Z"));
        TrackedLink successfulLink = new TrackedLink(
                11L,
                "https://github.com/user/successful",
                LinkSourceType.GITHUB,
                Instant.parse("2026-03-20T10:00:00Z"),
                Instant.parse("2026-03-20T10:00:00Z"),
                Instant.parse("2026-03-20T10:00:00Z"));

        when(linkRepository.findPage(0, 100)).thenReturn(List.of(failedLink, successfulLink));
        when(linkRepository.findPage(2, 100)).thenReturn(List.of());
        when(linkUpdater.supports(any())).thenReturn(true);
        when(linkUpdater.check(failedLink)).thenThrow(new RuntimeException("API unavailable"));
        when(linkUpdater.check(successfulLink))
                .thenReturn(LinkUpdateCheckResult.changed(
                        "GitHub issue created: Issue title", Instant.parse("2026-03-21T10:00:00Z")));
        when(subscriptionRepository.findChatIdsByLinkId(10L)).thenReturn(List.of(1L));
        when(subscriptionRepository.findChatIdsByLinkId(11L)).thenReturn(List.of(2L));

        linkPollingService.pollUpdates();

        verify(updatePublisher, org.mockito.Mockito.times(2)).publish(any(LinkUpdate.class));
    }
}
