package backend.academy.linktracker.scrapper.service.impl;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.service.UpdatePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications", name = "transport", havingValue = "HTTP")
public class BotHttpUpdatePublisher implements UpdatePublisher {

    private final BotClient botClient;

    @Override
    public void publish(LinkUpdate request) {
        botClient.sendUpdate(request);
    }
}
