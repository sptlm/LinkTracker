package backend.academy.linktracker.scrapper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = backend.academy.linktracker.scrapper.ScrapperApplication.class,
        properties = {
            "app.notifications.transport=KAFKA",
            "app.kafka.updates-topic=link-updates-it",
            "spring.flyway.enabled=false",
            "spring.task.scheduling.enabled=false"
        })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaUpdatePublisherIT {

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
    private UpdatePublisher updatePublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldPublishUpdateToKafkaTopic() throws Exception {
        LinkUpdate update = new LinkUpdate()
                .id(42L)
                .url(URI.create("https://github.com/example/repo"))
                .description("new commit")
                .tgChatIds(List.of(101L, 202L));

        updatePublisher.publish(update);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "scrapper-it-consumer",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            consumer.subscribe(List.of("link-updates-it"));

            LinkUpdate parsed = null;
            Instant deadline = Instant.now().plusSeconds(10);
            while (Instant.now().isBefore(deadline) && parsed == null) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    LinkUpdate candidate = objectMapper.readValue(record.value(), LinkUpdate.class);
                    if (Long.valueOf(42L).equals(candidate.getId())) {
                        parsed = candidate;
                        break;
                    }
                }
            }

            assertTrue(parsed != null, "Expected to find published update with id=42");
            assertNotNull(parsed);
            assertEquals(42L, parsed.getId());
            assertEquals("https://github.com/example/repo", String.valueOf(parsed.getUrl()));
            assertEquals("new commit", parsed.getDescription());
            assertEquals(List.of(101L, 202L), parsed.getTgChatIds());
        }
    }
}
