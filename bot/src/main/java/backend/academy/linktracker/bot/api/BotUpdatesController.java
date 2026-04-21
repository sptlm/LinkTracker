package backend.academy.linktracker.bot.api;

import backend.academy.linktracker.bot.generated.api.UpdatesApi;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.BotUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications", name = "transport", havingValue = "HTTP")
public class BotUpdatesController implements UpdatesApi {

    private final BotUpdateService botUpdateService;

    @Override
    @PostMapping("/updates")
    public ResponseEntity<Void> updatesPost(@Valid @RequestBody LinkUpdate request) {
        botUpdateService.processUpdate(request);
        return ResponseEntity.ok().build();
    }
}
