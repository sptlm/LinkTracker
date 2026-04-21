package backend.academy.linktracker.bot.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.contract.kafka.LinkUpdateAvroCodec;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
    "app.notifications.transport=KAFKA",
    "app.kafka.updates-topic=link-updates-bot-avro-it",
    "app.kafka.group-id=bot-avro-it-group",
    "app.kafka.payload-format=AVRO",
    "app.kafka.dlq-topic=link-updates-bot-avro-it-dlq",
    "app.kafka.max-attempts=3"
})
@ActiveProfiles("test")
class KafkaUpdateConsumerAvroIT {

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.1"));

    @Container
    static GenericContainer<?> schemaRegistry = new GenericContainer<>(DockerImageName.parse("confluentinc/cp-schema-registry:7.7.1"))
            .withExposedPorts(8081)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            ;

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        if (!kafkaContainer.isRunning()) {
            kafkaContainer.start();
        }
        if (!schemaRegistry.isRunning()) {
            schemaRegistry.withEnv(
                    "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                    "PLAINTEXT://host.testcontainers.internal:" + kafkaContainer.getMappedPort(9092));
            schemaRegistry.start();
        }

        registry.add("app.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("app.kafka.schema-registry-url", () -> "http://localhost:" + schemaRegistry.getMappedPort(8081));
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private LinkUpdateAvroCodec avroCodec;

    @MockitoBean
    private BotUpdateService botUpdateService;

    @Test
    void shouldConsumeAvroMessageAndForwardToUpdateService() {
        LinkUpdate update = new LinkUpdate()
                .id(808L)
                .url(URI.create("https://example.com/avro"))
                .description("avro payload")
                .tgChatIds(List.of(3L, 4L));

        kafkaTemplate.send("link-updates-bot-avro-it", "808", avroCodec.encodeToBase64(update));

        verify(botUpdateService, timeout(10_000))
                .processUpdate(argThat(it -> it.getId() == 808L
                        && "avro payload".equals(it.getDescription())
                        && it.getTgChatIds().equals(List.of(3L, 4L))));
    }
}
