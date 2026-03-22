package backend.academy.linktracker.scrapper.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.PersistenceProperties;
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
        PersistenceProperties persistenceProperties = new PersistenceProperties();
        persistenceProperties.setPollingBatchSize(100);
        linkPollingService = new LinkPollingService(
                linkRepository, subscriptionRepository, List.of(linkUpdater), updatePublisher, persistenceProperties);
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
        org.junit.jupiter.api.Assertions.assertEquals(List.of(1L, 3L), captor.getValue().getTgChatIds());
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
}
