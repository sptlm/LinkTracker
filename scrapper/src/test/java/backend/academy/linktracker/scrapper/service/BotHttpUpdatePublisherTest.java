package backend.academy.linktracker.scrapper.service;

import static org.mockito.Mockito.verify;

import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdateRequest;
import backend.academy.linktracker.scrapper.service.impl.BotHttpUpdatePublisher;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BotHttpUpdatePublisherTest {

    @Mock
    private BotClient botClient;

    @InjectMocks
    private BotHttpUpdatePublisher botHttpUpdatePublisher;

    @Test
    void publish_delegatesToBotClient() {
        LinkUpdateRequest request =
                new LinkUpdateRequest(1L, "https://github.com/user/repo", "Repository updated", List.of(1001L, 1002L));

        botHttpUpdatePublisher.publish(request);

        verify(botClient).sendUpdate(request);
    }
}
