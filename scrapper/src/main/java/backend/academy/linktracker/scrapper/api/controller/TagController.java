package backend.academy.linktracker.scrapper.api.controller;

import backend.academy.linktracker.scrapper.api.dto.LinkTagOperationRequest;
import backend.academy.linktracker.scrapper.api.dto.LinkTagsResponse;
import backend.academy.linktracker.scrapper.api.dto.LinkTagsUpdateRequest;
import backend.academy.linktracker.scrapper.service.TagService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<LinkTagsResponse> getTags(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestParam("link") URI link) {
        return ResponseEntity.ok(tagService.getTags(chatId, link));
    }

    @PostMapping
    public ResponseEntity<LinkTagsResponse> addTag(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody LinkTagOperationRequest request) {
        return ResponseEntity.ok(tagService.addTag(chatId, request));
    }

    @PutMapping
    public ResponseEntity<LinkTagsResponse> updateTags(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody LinkTagsUpdateRequest request) {
        return ResponseEntity.ok(tagService.updateTags(chatId, request));
    }

    @DeleteMapping
    public ResponseEntity<LinkTagsResponse> removeTag(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody LinkTagOperationRequest request) {
        return ResponseEntity.ok(tagService.removeTag(chatId, request));
    }
}
