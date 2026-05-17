package backend.academy.linktracker.scrapper.service;

import static backend.academy.linktracker.scrapper.configuration.ValkeyCacheConfiguration.LINK_LIST_CACHE;

import backend.academy.linktracker.scrapper.generated.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.generated.dto.LinksPost200Response;
import backend.academy.linktracker.scrapper.generated.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.generated.dto.RemoveLinkRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CachedLinkTrackingService {

    private final LinkTrackingService linkTrackingService;

    @Cacheable(cacheNames = LINK_LIST_CACHE, key = "'chat#' + #chatId")
    public ListLinksResponse getLinks(long chatId) {
        return linkTrackingService.getLinks(chatId);
    }

    @CacheEvict(cacheNames = LINK_LIST_CACHE, key = "'chat#' + #chatId")
    public LinksPost200Response addLink(long chatId, AddLinkRequest request) {
        return linkTrackingService.addLink(chatId, request);
    }

    @CacheEvict(cacheNames = LINK_LIST_CACHE, key = "'chat#' + #chatId")
    public LinksPost200Response removeLink(long chatId, RemoveLinkRequest request) {
        return linkTrackingService.removeLink(chatId, request);
    }
}
