package backend.academy.linktracker.scrapper.service.impl;

import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdateRequest;
import backend.academy.linktracker.scrapper.service.UpdatePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
public class BotHttpUpdatePublisher implements UpdatePublisher {

    private final BotClient botClient;

    @Override
    public void publish(LinkUpdateRequest request) {
        botClient.sendUpdate(request);
    }
}
