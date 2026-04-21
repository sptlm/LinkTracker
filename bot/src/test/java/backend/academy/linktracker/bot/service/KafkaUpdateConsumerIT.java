package backend.academy.linktracker.bot.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
    "app.kafka.group-id=bot-it-group"
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

    @Test
    void shouldConsumeMessageFromKafkaAndForwardToUpdateService() throws Exception {
        LinkUpdate update = new LinkUpdate()
                .id(99L)
                .url("https://stackoverflow.com/questions/123")
                .description("new answer")
                .tgChatIds(List.of(7L));

        kafkaTemplate.send("link-updates-bot-it", "99", objectMapper.writeValueAsString(update));

        verify(botUpdateService, timeout(10_000))
                .processUpdate(argThat(it -> it.getId() == 99L
                        && "new answer".equals(it.getDescription())
                        && it.getTgChatIds().equals(List.of(7L))));
    }
}
