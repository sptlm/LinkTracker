package backend.academy.linktracker.bot.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.AbstractKafkaIntegrationTest;
import backend.academy.linktracker.bot.TestcontainersConfiguration;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.contract.kafka.LinkUpdateAvroMapper;
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

@SpringBootTest(
        properties = {
            "app.notifications.transport=KAFKA",
            "app.kafka.updates-topic=link-updates-bot-avro-it",
            "app.kafka.group-id=bot-avro-it-group",
            "app.kafka.value-deserializer=io.confluent.kafka.serializers.KafkaAvroDeserializer",
            "app.kafka.dlq-value-serializer=io.confluent.kafka.serializers.KafkaAvroSerializer",
            "app.kafka.dlq-topic=link-updates-bot-avro-it-dlq",
            "app.kafka.dlq-topic-replication-factor=1",
            "app.kafka.max-attempts=3"
        })
@ActiveProfiles("test")
class KafkaUpdateConsumerAvroIT extends AbstractKafkaIntegrationTest {

    @DynamicPropertySource
    static void schemaRegistryProps(DynamicPropertyRegistry registry) {
        registry.add("app.kafka.schema-registry-url", TestcontainersConfiguration::schemaRegistryUrl);
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private LinkUpdateAvroMapper avroMapper;

    @MockitoBean
    private BotUpdateService botUpdateService;

    @Test
    void shouldConsumeAvroMessageAndForwardToUpdateService() {
        LinkUpdate update = new LinkUpdate()
                .id(808L)
                .url(URI.create("https://example.com/avro"))
                .description("avro payload")
                .tgChatIds(List.of(3L, 4L));

        kafkaTemplate.send("link-updates-bot-avro-it", "808", avroMapper.toRecord(update));

        verify(botUpdateService, timeout(10_000))
                .processUpdateForChat(
                        argThat(it -> it.getId() == 808L
                                && "avro payload".equals(it.getDescription())
                                && it.getTgChatIds().equals(List.of(3L, 4L))),
                        argThat(chatId -> chatId.equals(3L)));
    }
}
