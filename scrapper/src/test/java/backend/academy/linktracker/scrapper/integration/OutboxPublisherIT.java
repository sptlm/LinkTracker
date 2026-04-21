package backend.academy.linktracker.scrapper.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.outbox.KafkaOutboxSender;
import backend.academy.linktracker.scrapper.outbox.OutboxDispatcher;
import backend.academy.linktracker.scrapper.outbox.OutboxEvent;
import backend.academy.linktracker.scrapper.outbox.OutboxRepository;
import backend.academy.linktracker.scrapper.service.UpdatePublisher;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "app.notifications.transport=KAFKA",
    "app.kafka.bootstrap-servers=localhost:9092",
    "app.kafka.outbox-enabled=true",
    "app.kafka.outbox-dispatch-interval=10m"
})
class OutboxPublisherIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private UpdatePublisher updatePublisher;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxDispatcher outboxDispatcher;

    @Autowired
    private JdbcClient jdbcClient;

    @MockitoBean
    private KafkaOutboxSender kafkaOutboxSender;

    @BeforeEach
    void cleanOutbox() {
        jdbcClient.sql("truncate table notification_outbox restart identity").update();
        doNothing().when(kafkaOutboxSender).send(anyLong(), anyString());
    }

    @Test
    void shouldStoreEventInOutboxInsteadOfDirectPublish() {
        LinkUpdate update = new LinkUpdate()
                .id(10L)
                .url(URI.create("https://github.com/example/outbox"))
                .description("changed")
                .tgChatIds(List.of(1L, 2L));

        updatePublisher.publish(update);

        List<OutboxEvent> pending = outboxRepository.findPendingBatch(10);
        assertFalse(pending.isEmpty());
        assertEquals(1, pending.size());
        assertEquals(0, pending.getFirst().attempts());
    }

    @Test
    void shouldMarkEventAsSentAfterSuccessfulDispatch() {
        outboxRepository.enqueue("{\"id\":123}");

        outboxDispatcher.dispatchPending();

        Integer sentCount = jdbcClient
                .sql("select count(*) from notification_outbox where status='SENT'")
                .query(Integer.class)
                .single();
        assertEquals(1, sentCount);
    }

    @Test
    void shouldIncreaseAttemptsWhenDispatchFails() {
        outboxRepository.enqueue("{\"id\":456}");
        doThrow(new RuntimeException("kafka down")).when(kafkaOutboxSender).send(anyLong(), anyString());

        outboxDispatcher.dispatchPending();

        Integer attempts = jdbcClient
                .sql("select attempts from notification_outbox where id=1")
                .query(Integer.class)
                .single();
        assertEquals(1, attempts);
    }
}
