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

    @Override
    @PostMapping("/{id}")
    public ResponseEntity<Void> tgChatIdPost(@PathVariable("id") Long id) {
        chatService.register(id);
        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> tgChatIdDelete(@PathVariable("id") Long id) {
        chatService.delete(id);
        return ResponseEntity.ok().build();
    }
}
