package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.api.dto.LinkTagOperationRequest;
import backend.academy.linktracker.scrapper.api.dto.LinkTagsResponse;
import backend.academy.linktracker.scrapper.api.dto.LinkTagsUpdateRequest;
import backend.academy.linktracker.scrapper.api.exception.InvalidRequestException;
import backend.academy.linktracker.scrapper.api.exception.TagAlreadyAssignedException;
import backend.academy.linktracker.scrapper.api.exception.TagNotFoundException;
import backend.academy.linktracker.scrapper.api.exception.TrackedLinkNotFoundException;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.parser.ParsedLink;
import backend.academy.linktracker.scrapper.parser.SupportedLinkParser;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {

    private final ChatService chatService;
    private final LinkRepository linkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TagRepository tagRepository;
    private final SupportedLinkParser supportedLinkParser;

    @Transactional(readOnly = true)
    public LinkTagsResponse getTags(long chatId, URI link) {
        SubscriptionContext context = getSubscriptionContext(chatId, link);
        return toResponse(context.link(), tagRepository.findByChatIdAndLinkId(chatId, context.link().id()));
    }

    @Transactional
    public LinkTagsResponse addTag(long chatId, LinkTagOperationRequest request) {
        String tag = normalizeTag(request == null ? null : request.tag());
        SubscriptionContext context = getSubscriptionContext(chatId, request == null ? null : request.link());

        if (tagRepository.exists(chatId, context.link().id(), tag)) {
            throw new TagAlreadyAssignedException(tag, context.link().url(), chatId);
        }

        tagRepository.add(chatId, context.link().id(), tag);
        return toResponse(context.link(), tagRepository.findByChatIdAndLinkId(chatId, context.link().id()));
    }

    @Transactional
    public LinkTagsResponse updateTags(long chatId, LinkTagsUpdateRequest request) {
        SubscriptionContext context = getSubscriptionContext(chatId, request == null ? null : request.link());
        List<String> tags = normalizeTags(request == null ? null : request.tags());
        tagRepository.replace(chatId, context.link().id(), tags);
        return toResponse(context.link(), tagRepository.findByChatIdAndLinkId(chatId, context.link().id()));
    }

    @Transactional
    public LinkTagsResponse removeTag(long chatId, LinkTagOperationRequest request) {
        String tag = normalizeTag(request == null ? null : request.tag());
        SubscriptionContext context = getSubscriptionContext(chatId, request == null ? null : request.link());

        if (!tagRepository.exists(chatId, context.link().id(), tag)) {
            throw new TagNotFoundException(tag, context.link().url(), chatId);
        }

        tagRepository.delete(chatId, context.link().id(), tag);
        return toResponse(context.link(), tagRepository.findByChatIdAndLinkId(chatId, context.link().id()));
    }

    private SubscriptionContext getSubscriptionContext(long chatId, URI link) {
        chatService.ensureExists(chatId);

        if (link == null || link.toString().isBlank()) {
            throw new InvalidRequestException("Ссылка обязательна");
        }

        ParsedLink parsedLink = supportedLinkParser.parse(link.toString());
        TrackedLink trackedLink = linkRepository
                .findByUrl(parsedLink.normalizedUrl())
                .orElseThrow(() -> new TrackedLinkNotFoundException(parsedLink.normalizedUrl(), chatId));

        subscriptionRepository
                .findByChatIdAndLinkId(chatId, trackedLink.id())
                .orElseThrow(() -> new TrackedLinkNotFoundException(parsedLink.normalizedUrl(), chatId));

        return new SubscriptionContext(trackedLink);
    }

    private String normalizeTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new InvalidRequestException("Тег обязателен");
        }
        return tag.trim();
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        values -> values.stream().sorted().toList()));
    }

    private LinkTagsResponse toResponse(TrackedLink trackedLink, List<String> tags) {
        return new LinkTagsResponse(trackedLink.id(), URI.create(trackedLink.url()), tags);
    }

    private record SubscriptionContext(TrackedLink link) {}
}
