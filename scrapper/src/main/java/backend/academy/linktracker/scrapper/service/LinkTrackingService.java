package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.generated.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.generated.dto.LinksPost200Response;
import backend.academy.linktracker.scrapper.generated.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.generated.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.api.exception.InvalidRequestException;
import backend.academy.linktracker.scrapper.api.exception.LinkAlreadyTrackedException;
import backend.academy.linktracker.scrapper.api.exception.TrackedLinkNotFoundException;
import backend.academy.linktracker.scrapper.model.LinkSubscription;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.parser.ParsedLink;
import backend.academy.linktracker.scrapper.parser.SupportedLinkParser;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkTrackingService {

    private final ChatService chatService;
    private final LinkRepository linkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SupportedLinkParser supportedLinkParser;

    public LinksPost200Response addLink(long chatId, AddLinkRequest request) {
        chatService.ensureExists(chatId);

        if (request == null || request.getLink() == null || request.getLink().toString().isBlank()) {
            throw new InvalidRequestException("Ссылка обязательна");
        }

        ParsedLink parsedLink = supportedLinkParser.parse(request.getLink().toString());
        TrackedLink trackedLink = findOrCreateLink(parsedLink);

        if (subscriptionRepository.exists(chatId, trackedLink.id())) {
            throw new LinkAlreadyTrackedException(trackedLink.url(), chatId);
        }

        LinkSubscription subscription = new LinkSubscription(
                chatId,
                trackedLink.id(),
                normalizeList(request.getTags()),
                normalizeList(request.getFilters()),
                Instant.now());

        subscriptionRepository.save(subscription);
        return toResponse(trackedLink, subscription);
    }

    public LinksPost200Response removeLink(long chatId, RemoveLinkRequest request) {
        chatService.ensureExists(chatId);

        if (request == null || request.getLink() == null || request.getLink().toString().isBlank()) {
            throw new InvalidRequestException("Ссылка обязательна");
        }

        ParsedLink parsedLink = supportedLinkParser.parse(request.getLink().toString());

        TrackedLink trackedLink = linkRepository
                .findByUrl(parsedLink.normalizedUrl())
                .orElseThrow(() -> new TrackedLinkNotFoundException(parsedLink.normalizedUrl(), chatId));

        LinkSubscription subscription = subscriptionRepository
                .findByChatIdAndLinkId(chatId, trackedLink.id())
                .orElseThrow(() -> new TrackedLinkNotFoundException(parsedLink.normalizedUrl(), chatId));

        subscriptionRepository.delete(chatId, trackedLink.id());

        if (!subscriptionRepository.hasSubscribers(trackedLink.id())) {
            linkRepository.deleteById(trackedLink.id());
        }

        return toResponse(trackedLink, subscription);
    }

    public ListLinksResponse getLinks(long chatId) {
        chatService.ensureExists(chatId);

        List<LinksPost200Response> links = subscriptionRepository.findByChatId(chatId).stream()
                .map(subscription -> linkRepository
                        .findById(subscription.linkId())
                        .map(link -> toResponse(link, subscription))
                        .orElse(null))
                .filter(response -> response != null)
                .toList();

        return new ListLinksResponse().links(links).size(links.size());
    }

    private TrackedLink findOrCreateLink(ParsedLink parsedLink) {
        return linkRepository
                .findByUrl(parsedLink.normalizedUrl())
                .orElseGet(() -> linkRepository.save(new TrackedLink(
                        linkRepository.nextId(),
                        parsedLink.normalizedUrl(),
                        parsedLink.type(),
                        Instant.now(),
                        null,
                        null)));
    }

    private LinksPost200Response toResponse(TrackedLink link, LinkSubscription subscription) {
        return new LinksPost200Response().id(link.id()).url(URI.create(link.url())).tags(subscription.tags()).filters(subscription.filters());
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(val -> val != null && !val.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new), List::copyOf));
    }
}
