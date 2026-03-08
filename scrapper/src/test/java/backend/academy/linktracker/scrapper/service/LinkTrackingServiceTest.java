package backend.academy.linktracker.scrapper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.api.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.api.dto.LinkResponse;
import backend.academy.linktracker.scrapper.api.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.api.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.api.exception.InvalidRequestException;
import backend.academy.linktracker.scrapper.api.exception.LinkAlreadyTrackedException;
import backend.academy.linktracker.scrapper.api.exception.TrackedLinkNotFoundException;
import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.LinkSubscription;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.parser.ParsedLink;
import backend.academy.linktracker.scrapper.parser.SupportedLinkParser;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkTrackingServiceTest {

    @Mock
    private ChatService chatService;

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SupportedLinkParser supportedLinkParser;

    @InjectMocks
    private LinkTrackingService linkTrackingService;

    @Test
    void addLink_whenRequestIsNull_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class, () -> linkTrackingService.addLink(123L, null));

        verify(chatService).ensureExists(123L);
    }

    @Test
    void addLink_whenLinkIsBlank_throwsInvalidRequestException() {
        AddLinkRequest request = new AddLinkRequest("   ", List.of(), List.of());

        assertThrows(InvalidRequestException.class, () -> linkTrackingService.addLink(123L, request));

        verify(chatService).ensureExists(123L);
    }

    @Test
    void addLink_whenAlreadyTracked_throwsLinkAlreadyTrackedException() {
        AddLinkRequest request =
                new AddLinkRequest("https://github.com/user/repo", List.of("java"), List.of("branch=main"));

        ParsedLink parsedLink = org.mockito.Mockito.mock(ParsedLink.class);
        TrackedLink trackedLink =
                new TrackedLink(10L, "https://github.com/user/repo", LinkSourceType.GITHUB, Instant.now(), null, null);

        when(supportedLinkParser.parse("https://github.com/user/repo")).thenReturn(parsedLink);
        when(parsedLink.normalizedUrl()).thenReturn("https://github.com/user/repo");
        when(linkRepository.findByUrl("https://github.com/user/repo")).thenReturn(Optional.of(trackedLink));
        when(subscriptionRepository.exists(123L, 10L)).thenReturn(true);

        assertThrows(LinkAlreadyTrackedException.class, () -> linkTrackingService.addLink(123L, request));
    }

    @Test
    void removeLink_whenRequestIsBlank_throwsInvalidRequestException() {
        RemoveLinkRequest request = new RemoveLinkRequest(" ");

        assertThrows(InvalidRequestException.class, () -> linkTrackingService.removeLink(123L, request));

        verify(chatService).ensureExists(123L);
    }

    @Test
    void removeLink_whenTrackedLinkDoesNotExist_throwsTrackedLinkNotFoundException() {
        RemoveLinkRequest request = new RemoveLinkRequest("https://github.com/user/repo");
        ParsedLink parsedLink = org.mockito.Mockito.mock(ParsedLink.class);

        when(supportedLinkParser.parse("https://github.com/user/repo")).thenReturn(parsedLink);
        when(parsedLink.normalizedUrl()).thenReturn("https://github.com/user/repo");
        when(linkRepository.findByUrl("https://github.com/user/repo")).thenReturn(Optional.empty());

        assertThrows(TrackedLinkNotFoundException.class, () -> linkTrackingService.removeLink(123L, request));
    }

    @Test
    void removeLink_whenSubscriptionDoesNotExist_throwsTrackedLinkNotFoundException() {
        RemoveLinkRequest request = new RemoveLinkRequest("https://github.com/user/repo");
        ParsedLink parsedLink = org.mockito.Mockito.mock(ParsedLink.class);
        TrackedLink trackedLink =
                new TrackedLink(10L, "https://github.com/user/repo", LinkSourceType.GITHUB, Instant.now(), null, null);

        when(supportedLinkParser.parse("https://github.com/user/repo")).thenReturn(parsedLink);
        when(parsedLink.normalizedUrl()).thenReturn("https://github.com/user/repo");
        when(linkRepository.findByUrl("https://github.com/user/repo")).thenReturn(Optional.of(trackedLink));
        when(subscriptionRepository.findByChatIdAndLinkId(123L, 10L)).thenReturn(Optional.empty());

        assertThrows(TrackedLinkNotFoundException.class, () -> linkTrackingService.removeLink(123L, request));
    }

    @Test
    void removeLink_whenLastSubscriber_deletesSubscriptionAndLink() {
        RemoveLinkRequest request = new RemoveLinkRequest("https://github.com/user/repo");
        ParsedLink parsedLink = org.mockito.Mockito.mock(ParsedLink.class);
        TrackedLink trackedLink =
                new TrackedLink(10L, "https://github.com/user/repo", LinkSourceType.GITHUB, Instant.now(), null, null);
        LinkSubscription subscription =
                new LinkSubscription(123L, 10L, List.of("java"), List.of("branch=main"), Instant.now());

        when(supportedLinkParser.parse("https://github.com/user/repo")).thenReturn(parsedLink);
        when(parsedLink.normalizedUrl()).thenReturn("https://github.com/user/repo");
        when(linkRepository.findByUrl("https://github.com/user/repo")).thenReturn(Optional.of(trackedLink));
        when(subscriptionRepository.findByChatIdAndLinkId(123L, 10L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.hasSubscribers(10L)).thenReturn(false);

        LinkResponse response = linkTrackingService.removeLink(123L, request);

        verify(subscriptionRepository).delete(123L, 10L);
        verify(linkRepository).deleteById(10L);
        assertEquals(10L, response.id());
        assertEquals("https://github.com/user/repo", response.url());
        assertEquals(List.of("java"), response.tags());
        assertEquals(List.of("branch=main"), response.filters());
    }

    @Test
    void removeLink_whenOtherSubscribersRemain_deletesOnlySubscription() {
        RemoveLinkRequest request = new RemoveLinkRequest("https://github.com/user/repo");
        ParsedLink parsedLink = org.mockito.Mockito.mock(ParsedLink.class);
        TrackedLink trackedLink =
                new TrackedLink(10L, "https://github.com/user/repo", LinkSourceType.GITHUB, Instant.now(), null, null);
        LinkSubscription subscription = new LinkSubscription(123L, 10L, List.of("java"), List.of(), Instant.now());

        when(supportedLinkParser.parse("https://github.com/user/repo")).thenReturn(parsedLink);
        when(parsedLink.normalizedUrl()).thenReturn("https://github.com/user/repo");
        when(linkRepository.findByUrl("https://github.com/user/repo")).thenReturn(Optional.of(trackedLink));
        when(subscriptionRepository.findByChatIdAndLinkId(123L, 10L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.hasSubscribers(10L)).thenReturn(true);

        linkTrackingService.removeLink(123L, request);

        verify(subscriptionRepository).delete(123L, 10L);
        verify(linkRepository, never()).deleteById(10L);
    }

    @Test
    void getLinks_returnsOnlyExistingLinks() {
        LinkSubscription existingSubscription =
                new LinkSubscription(123L, 10L, List.of("java"), List.of("branch=main"), Instant.now());
        LinkSubscription missingLinkSubscription =
                new LinkSubscription(123L, 11L, List.of("sql"), List.of(), Instant.now());

        TrackedLink trackedLink =
                new TrackedLink(10L, "https://github.com/user/repo", LinkSourceType.GITHUB, Instant.now(), null, null);

        when(subscriptionRepository.findByChatId(123L))
                .thenReturn(List.of(existingSubscription, missingLinkSubscription));
        when(linkRepository.findById(10L)).thenReturn(Optional.of(trackedLink));
        when(linkRepository.findById(11L)).thenReturn(Optional.empty());

        ListLinksResponse response = linkTrackingService.getLinks(123L);

        verify(chatService).ensureExists(123L);
        assertEquals(1, response.size());
        assertEquals(1, response.links().size());
        assertEquals("https://github.com/user/repo", response.links().getFirst().url());
        assertEquals(List.of("java"), response.links().getFirst().tags());
        assertEquals(List.of("branch=main"), response.links().getFirst().filters());
    }
}
