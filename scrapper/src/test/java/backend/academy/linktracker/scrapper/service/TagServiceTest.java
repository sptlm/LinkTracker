package backend.academy.linktracker.scrapper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.api.dto.LinkTagOperationRequest;
import backend.academy.linktracker.scrapper.api.dto.LinkTagsResponse;
import backend.academy.linktracker.scrapper.api.dto.LinkTagsUpdateRequest;
import backend.academy.linktracker.scrapper.api.exception.InvalidRequestException;
import backend.academy.linktracker.scrapper.api.exception.TagAlreadyAssignedException;
import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.LinkSubscription;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.parser.ParsedLink;
import backend.academy.linktracker.scrapper.parser.SupportedLinkParser;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private ChatService chatService;

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private SupportedLinkParser supportedLinkParser;

    @Mock
    private LinkListCacheService linkListCacheService;

    @InjectMocks
    private TagService tagService;

    @Test
    void addTag_whenTagAlreadyExists_throwsConflict() {
        URI link = URI.create("https://github.com/user/repo");
        TrackedLink trackedLink = trackedLink();

        when(supportedLinkParser.parse(link.toString()))
                .thenReturn(new ParsedLink(trackedLink.url(), LinkSourceType.GITHUB));
        when(linkRepository.findByUrl(trackedLink.url())).thenReturn(Optional.of(trackedLink));
        when(subscriptionRepository.findByChatIdAndLinkId(1L, trackedLink.id()))
                .thenReturn(Optional.of(new LinkSubscription(1L, trackedLink.id(), List.of("java"), Instant.now())));
        when(tagRepository.exists(1L, trackedLink.id(), "java")).thenReturn(true);

        assertThrows(
                TagAlreadyAssignedException.class,
                () -> tagService.addTag(1L, new LinkTagOperationRequest(link, "java")));
    }

    @Test
    void updateTags_normalizesAndReturnsSortedTags() {
        URI link = URI.create("https://github.com/user/repo");
        TrackedLink trackedLink = trackedLink();

        when(supportedLinkParser.parse(link.toString()))
                .thenReturn(new ParsedLink(trackedLink.url(), LinkSourceType.GITHUB));
        when(linkRepository.findByUrl(trackedLink.url())).thenReturn(Optional.of(trackedLink));
        when(subscriptionRepository.findByChatIdAndLinkId(1L, trackedLink.id()))
                .thenReturn(Optional.of(new LinkSubscription(1L, trackedLink.id(), List.of("java"), Instant.now())));
        when(tagRepository.findByChatIdAndLinkId(1L, trackedLink.id())).thenReturn(List.of("orm", "sql"));

        LinkTagsResponse response =
                tagService.updateTags(1L, new LinkTagsUpdateRequest(link, List.of("sql", "orm", "sql", " ")));

        verify(tagRepository).replace(1L, trackedLink.id(), List.of("orm", "sql"));
        assertEquals(List.of("orm", "sql"), response.tags());
    }

    @Test
    void removeTag_whenTagBlank_throwsBadRequest() {
        assertThrows(
                InvalidRequestException.class,
                () -> tagService.removeTag(
                        1L, new LinkTagOperationRequest(URI.create("https://github.com/user/repo"), " ")));
    }

    private TrackedLink trackedLink() {
        return new TrackedLink(
                10L,
                "https://github.com/user/repo",
                LinkSourceType.GITHUB,
                Instant.parse("2024-01-01T00:00:00Z"),
                null,
                null);
    }
}
