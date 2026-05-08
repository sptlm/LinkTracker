package backend.academy.linktracker.bot.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.AbstractKafkaIntegrationTest;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        properties = {
            "app.notifications.transport=KAFKA",
            "app.kafka.updates-topic=link-updates-bot-it",
            "app.kafka.group-id=bot-it-group",
            "app.kafka.dlq-topic=link-updates-bot-it-dlq",
            "app.kafka.dlq-topic-replication-factor=1",
            "app.kafka.max-attempts=3"
        })
@ActiveProfiles("test")
class KafkaUpdateConsumerIT extends AbstractKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BotUpdateService botUpdateService;

    @Test
    void shouldConsumeMessageFromKafkaAndForwardToUpdateService() throws Exception {
        LinkUpdate update = new LinkUpdate()
                .id(99L)
                .url(URI.create("https://stackoverflow.com/questions/123"))
                .description("new answer")
                .tgChatIds(List.of(7L));

        kafkaTemplate.send("link-updates-bot-it", "99", objectMapper.writeValueAsString(update));

        verify(botUpdateService, timeout(10_000))
                .processUpdateForChat(
                        argThat(it -> it.getId() == 99L
                                && "new answer".equals(it.getDescription())
                                && it.getTgChatIds().equals(List.of(7L))),
                        argThat(chatId -> chatId.equals(7L)));
    }

    @Test
    void shouldSendMalformedPayloadToDlqWithoutRetries() {
        kafkaTemplate.send("link-updates-bot-it", "bad-json", "{not-json}");

        ConsumerRecord<String, String> dlqRecord = pollSingleRecord("link-updates-bot-it-dlq", Duration.ofSeconds(10));
        assertNotNull(dlqRecord);
        assertTrue(dlqRecord.value().contains("{not-json}"));
    }

    @Test
    void shouldSendValidationErrorPayloadToDlqWithoutRetries() {
        kafkaTemplate.send(
                "link-updates-bot-it",
                "bad-validation",
                "{\"id\": 15, \"url\": \"https://example.com\", \"description\": \"\", \"tgChatIds\": [1]}");

        ConsumerRecord<String, String> dlqRecord = pollSingleRecord("link-updates-bot-it-dlq", Duration.ofSeconds(10));
        assertNotNull(dlqRecord);
        assertTrue(dlqRecord.value().contains("\"id\": 15"));
    }

    @Test
    void shouldRetryBusinessErrorAndThenSendToDlq() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
                    attempts.incrementAndGet();
                    throw new RuntimeException("temporary error");
                })
                .when(botUpdateService)
                .processUpdateForChat(any(), any());

        LinkUpdate update = new LinkUpdate()
                .id(777L)
                .url(URI.create("https://github.com/example/retries"))
                .description("boom")
                .tgChatIds(List.of(100L));

        kafkaTemplate.send("link-updates-bot-it", "777", objectMapper.writeValueAsString(update));

        verify(botUpdateService, timeout(10_000).atLeast(3)).processUpdateForChat(any(), any());

        ConsumerRecord<String, String> dlqRecord = pollSingleRecord("link-updates-bot-it-dlq", Duration.ofSeconds(10));
        assertNotNull(dlqRecord);
        assertTrue(dlqRecord.value().contains("\"id\":777"));
        assertTrue(attempts.get() >= 3);
    }

    @Test
    void shouldProcessDifferentKafkaMessagesEvenWithSamePayload() throws Exception {
        LinkUpdate update = new LinkUpdate()
                .id(888L)
                .url(URI.create("https://example.com/dup"))
                .description("same payload")
                .tgChatIds(List.of(42L));

        String payload = objectMapper.writeValueAsString(update);
        kafkaTemplate.send("link-updates-bot-it", "888", payload);
        kafkaTemplate.send("link-updates-bot-it", "888", payload);

        verify(botUpdateService, timeout(10_000).times(2)).processUpdateForChat(any(), any());
    }

    private ConsumerRecord<String, String> pollSingleRecord(String topic, Duration timeout) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "bot-it-dlq-consumer-" + System.nanoTime(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class))) {
            consumer.subscribe(List.of(topic));

            long deadline = System.currentTimeMillis() + timeout.toMillis();
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    return record;
                }
            }
        }

        return null;
    }
}
