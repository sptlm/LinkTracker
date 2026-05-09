package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.generated.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.generated.dto.LinksPost200Response;
import backend.academy.linktracker.scrapper.generated.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.generated.dto.RemoveLinkRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CachedLinkTrackingService {

    private final LinkTrackingService linkTrackingService;
    private final LinkListCacheService cacheService;

    public ListLinksResponse getLinks(long chatId) {
        return cacheService.get(chatId).orElseGet(() -> {
            ListLinksResponse response = linkTrackingService.getLinks(chatId);
            cacheService.put(chatId, response);
            return response;
        });
    }

    public LinksPost200Response addLink(long chatId, AddLinkRequest request) {
        LinksPost200Response response = linkTrackingService.addLink(chatId, request);
        cacheService.evictAfterCommit(chatId);
        return response;
    }

    public LinksPost200Response removeLink(long chatId, RemoveLinkRequest request) {
        LinksPost200Response response = linkTrackingService.removeLink(chatId, request);
        cacheService.evictAfterCommit(chatId);
        return response;
    }
}
