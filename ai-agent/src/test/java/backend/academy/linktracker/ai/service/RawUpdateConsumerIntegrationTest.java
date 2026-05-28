package backend.academy.linktracker.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import backend.academy.linktracker.ai.AbstractKafkaIntegrationTest;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        properties = {
            "app.kafka.raw-updates-topic=link-raw-updates-ai-it",
            "app.kafka.processed-updates-topic=link-processed-updates-ai-it",
            "app.kafka.group-id=ai-agent-it-group",
            "app.kafka.topics-partitions=1",
            "app.kafka.topics-replication-factor=1",
            "ai-agent.filtering.stop-words[0]=spam",
            "ai-agent.filtering.excluded-authors[0]=bot-user",
            "ai-agent.filtering.min-length=5",
            "ai-agent.summarization.provider=STUB",
            "ai-agent.summarization.threshold=12",
            "ai-agent.prioritization.high-keywords[0]=critical",
            "ai-agent.prioritization.high-keywords[1]=urgent",
            "ai-agent.prioritization.low-keywords[0]=typo",
            "ai-agent.prioritization.low-keywords[1]=docs",
            "ai-agent.grouping.window-ms=50ms"
        })
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class RawUpdateConsumerIntegrationTest extends AbstractKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldConsumeRawUpdateAndPublishProcessedUpdate() throws Exception {
        LinkUpdate update = new LinkUpdate()
                .id(101L)
                .url(URI.create("https://github.com/example/repo"))
                .description("long update text")
                .author("alice")
                .tgChatIds(List.of(10L));

        kafkaTemplate
                .send("link-raw-updates-ai-it", "101", objectMapper.writeValueAsString(update))
                .get();

        ConsumerRecord<String, String> record =
                pollSingleRecord("link-processed-updates-ai-it", "101", Duration.ofSeconds(10));
        assertNotNull(record);

        LinkUpdate processed = objectMapper.readValue(record.value(), LinkUpdate.class);
        assertEquals(101L, processed.getId());
        assertEquals("long update ...", processed.getDescription());
        assertEquals("alice", processed.getAuthor());
        assertEquals("MEDIUM", processed.getPriority());
        assertEquals(List.of(10L), processed.getTgChatIds());
    }

    @Test
    void shouldSkipMalformedPayloadWithoutStoppingConsumer(CapturedOutput output) throws Exception {
        kafkaTemplate.send("link-raw-updates-ai-it", "bad-json", "{not-json}").get();

        LinkUpdate update = new LinkUpdate()
                .id(202L)
                .url(URI.create("https://stackoverflow.com/questions/1"))
                .description("valid update")
                .author("alice")
                .tgChatIds(List.of(30L));
        kafkaTemplate
                .send("link-raw-updates-ai-it", "202", objectMapper.writeValueAsString(update))
                .get();

        ConsumerRecord<String, String> record =
                pollSingleRecord("link-processed-updates-ai-it", "202", Duration.ofSeconds(10));
        assertNotNull(record);

        LinkUpdate processed = objectMapper.readValue(record.value(), LinkUpdate.class);
        assertEquals(202L, processed.getId());
        assertEquals("valid update", processed.getDescription());
        assertEquals("MEDIUM", processed.getPriority());
        assertThat(output.toString()).contains("Failed to deserialize raw update");
    }

    @Test
    void shouldGroupRawUpdatesForSameChatAndPublishSingleProcessedUpdate() throws Exception {
        LinkUpdate first = new LinkUpdate()
                .id(501L)
                .url(URI.create("https://example.com/first"))
                .description("docs update text")
                .author("alice")
                .tgChatIds(List.of(50L));
        LinkUpdate second = new LinkUpdate()
                .id(502L)
                .url(URI.create("https://example.com/second"))
                .description("critical update text")
                .author("bob")
                .tgChatIds(List.of(50L));

        kafkaTemplate
                .send("link-raw-updates-ai-it", "501", objectMapper.writeValueAsString(first))
                .get();
        kafkaTemplate
                .send("link-raw-updates-ai-it", "502", objectMapper.writeValueAsString(second))
                .get();

        ConsumerRecord<String, String> record =
                pollSingleRecord("link-processed-updates-ai-it", "501", Duration.ofSeconds(10));
        assertNotNull(record);

        LinkUpdate processed = objectMapper.readValue(record.value(), LinkUpdate.class);
        assertEquals(501L, processed.getId());
        assertEquals("1. docs update ..." + System.lineSeparator() + "2. critical upd...", processed.getDescription());
        assertEquals(List.of(50L), processed.getTgChatIds());
        assertEquals("HIGH", processed.getPriority());
    }

    @Test
    void shouldNotPublishFilteredUpdate() throws Exception {
        LinkUpdate update = new LinkUpdate()
                .id(303L)
                .url(URI.create("https://example.com/spam"))
                .description("this update contains spam content")
                .author("alice")
                .tgChatIds(List.of(40L));

        kafkaTemplate
                .send("link-raw-updates-ai-it", "303", objectMapper.writeValueAsString(update))
                .get();

        assertNull(pollSingleRecord("link-processed-updates-ai-it", "303", Duration.ofSeconds(3)));
    }

    @Test
    void shouldNotPublishUpdateWithoutChats() throws Exception {
        LinkUpdate update = new LinkUpdate()
                .id(404L)
                .url(URI.create("https://example.com/no-chats"))
                .description("valid update text")
                .author("alice")
                .tgChatIds(List.of());

        kafkaTemplate
                .send("link-raw-updates-ai-it", "404", objectMapper.writeValueAsString(update))
                .get();

        assertNull(pollSingleRecord("link-processed-updates-ai-it", "404", Duration.ofSeconds(3)));
    }

    private ConsumerRecord<String, String> pollSingleRecord(String topic, String expectedKey, Duration timeout) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "ai-agent-it-consumer-" + System.nanoTime(),
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
                    if (expectedKey.equals(record.key())) {
                        return record;
                    }
                }
            }
        }

        return null;
    }
}
