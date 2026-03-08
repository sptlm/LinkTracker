package backend.academy.linktracker.bot.api;

import backend.academy.linktracker.bot.api.dto.LinkUpdateRequest;
import backend.academy.linktracker.bot.service.BotUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BotUpdatesController {

    private final BotUpdateService botUpdateService;

    @PostMapping("/updates")
    public ResponseEntity<Void> updates(@Valid @RequestBody LinkUpdateRequest request) {
        botUpdateService.processUpdate(request);
        return ResponseEntity.ok().build();
    }
}
