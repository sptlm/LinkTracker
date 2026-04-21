package backend.academy.linktracker.bot.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
    "app.notifications.transport=KAFKA",
    "app.kafka.updates-topic=link-updates-bot-it",
    "app.kafka.group-id=bot-it-group",
    "app.kafka.dlq-topic=link-updates-bot-it-dlq",
    "app.kafka.max-attempts=3"
})
@ActiveProfiles("test")
class KafkaUpdateConsumerIT {

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.1"));

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        if (!kafkaContainer.isRunning()) {
            kafkaContainer.start();
        }

        registry.add("app.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BotUpdateService botUpdateService;

    @BeforeEach
    void resetDefaultBehavior() {
        doNothing().when(botUpdateService).processUpdate(any());
    }

    @Test
    void shouldConsumeMessageFromKafkaAndForwardToUpdateService() throws Exception {
        LinkUpdate update = new LinkUpdate()
                .id(99L)
                .url(URI.create("https://stackoverflow.com/questions/123"))
                .description("new answer")
                .tgChatIds(List.of(7L));

        kafkaTemplate.send("link-updates-bot-it", "99", objectMapper.writeValueAsString(update));

        verify(botUpdateService, timeout(10_000))
                .processUpdate(argThat(it -> it.getId() == 99L
                        && "new answer".equals(it.getDescription())
                        && it.getTgChatIds().equals(List.of(7L))));
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
                .processUpdate(any());

        LinkUpdate update = new LinkUpdate()
                .id(777L)
                .url(URI.create("https://github.com/example/retries"))
                .description("boom")
                .tgChatIds(List.of(100L));

        kafkaTemplate.send("link-updates-bot-it", "777", objectMapper.writeValueAsString(update));

        verify(botUpdateService, timeout(10_000).atLeast(3)).processUpdate(any());

        ConsumerRecord<String, String> dlqRecord = pollSingleRecord("link-updates-bot-it-dlq", Duration.ofSeconds(10));
        assertNotNull(dlqRecord);
        assertTrue(dlqRecord.value().contains("\"id\":777"));
        assertTrue(attempts.get() >= 3);
    }

    private ConsumerRecord<String, String> pollSingleRecord(String topic, Duration timeout) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "bot-it-dlq-consumer-" + System.nanoTime(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
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
