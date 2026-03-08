package backend.academy.linktracker.scrapper.api.controller;

import backend.academy.linktracker.scrapper.api.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.api.dto.LinkResponse;
import backend.academy.linktracker.scrapper.api.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.api.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.service.LinkTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class LinkController {

    private final LinkTrackingService linkTrackingService;

    @GetMapping
    public ListLinksResponse getLinks(@RequestHeader("Tg-Chat-Id") long chatId) {
        return linkTrackingService.getLinks(chatId);
    }

    @PostMapping
    public LinkResponse addLink(@RequestHeader("Tg-Chat-Id") long chatId, @Valid @RequestBody AddLinkRequest request) {
        return linkTrackingService.addLink(chatId, request);
    }

    @DeleteMapping
    public LinkResponse removeLink(
            @RequestHeader("Tg-Chat-Id") long chatId, @Valid @RequestBody RemoveLinkRequest request) {
        return linkTrackingService.removeLink(chatId, request);
    }
}
