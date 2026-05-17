package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.integration.AbstractPostgresIT;
import backend.academy.linktracker.scrapper.outbox.OutboxRepository;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(
        properties = {
            "app.notifications.transport=HTTP",
            "spring.task.scheduling.enabled=false",
            "app.kafka.outbox-dispatch-enabled=false"
        })
class HttpNotificationFallbackIT extends AbstractPostgresIT {

    @Autowired
    private UpdatePublisher updatePublisher;

    @Autowired
    private OutboxRepository outboxRepository;

    @MockitoBean
    private BotClient botClient;

    @Test
    void shouldQueueUpdateToKafkaOutboxWhenPrimaryHttpTransportFails() {
        LinkUpdate update = new LinkUpdate()
                .id(77L)
                .url(URI.create("https://github.com/example/repo"))
                .description("fallback")
                .tgChatIds(List.of(123L));
        doThrow(new RuntimeException("bot is down")).when(botClient).sendUpdate(update);

        updatePublisher.publish(update);

        var pending = outboxRepository.findPendingBatch(10, 3);
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().payload()).contains("\"id\":77");
    }
}
