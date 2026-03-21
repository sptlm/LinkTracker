package backend.academy.linktracker.scrapper.api.controller;

import backend.academy.linktracker.scrapper.generated.api.TgChatApi;
import backend.academy.linktracker.scrapper.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tg-chat")
@RequiredArgsConstructor
public class TgChatController implements TgChatApi {

    private final ChatService chatService;

    @PostMapping("/{id}")
    public ResponseEntity<Void> tgChatIdPost(@PathVariable("id") Long chatId) {
        chatService.register(chatId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> tgChatIdDelete(@PathVariable("id") Long chatId) {
        chatService.delete(chatId);
        return ResponseEntity.ok().build();
    }
}
