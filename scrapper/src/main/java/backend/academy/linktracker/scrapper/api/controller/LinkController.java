package backend.academy.linktracker.scrapper.api.controller;

import backend.academy.linktracker.scrapper.generated.api.LinksApi;
import backend.academy.linktracker.scrapper.generated.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.generated.dto.LinksPost200Response;
import backend.academy.linktracker.scrapper.generated.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.generated.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.service.LinkTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/links")
@RequiredArgsConstructor
public class LinkController implements LinksApi {

    private final LinkTrackingService linkTrackingService;

    @GetMapping
    public ResponseEntity<ListLinksResponse> linksGet(@RequestHeader("Tg-Chat-Id") Long chatId) {
        return ResponseEntity.ok(linkTrackingService.getLinks(chatId));
    }

    @PostMapping
    public ResponseEntity<LinksPost200Response> linksPost(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody AddLinkRequest request) {
        return ResponseEntity.ok(linkTrackingService.addLink(chatId, request));
    }

    @DeleteMapping
    public ResponseEntity<LinksPost200Response> linksDelete(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody RemoveLinkRequest request) {
        return ResponseEntity.ok(linkTrackingService.removeLink(chatId, request));
    }
}
