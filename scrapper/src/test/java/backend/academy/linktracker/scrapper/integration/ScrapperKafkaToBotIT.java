package backend.academy.linktracker.scrapper.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.BotApplication;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.ScrapperApplication;
import backend.academy.linktracker.scrapper.service.UpdatePublisher;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = {ScrapperApplication.class, BotApplication.class},
        properties = {
            "app.notifications.transport=KAFKA",
            "app.kafka.updates-topic=scrapper-to-bot-it",
            "app.kafka.group-id=scrapper-to-bot-group",
            "spring.flyway.enabled=false",
            "app.telegram.token=test-token"
        })
class ScrapperKafkaToBotIT {

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.1"));

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("app.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private UpdatePublisher updatePublisher;

    @MockitoBean
    private TelegramBot telegramBot;

    @Test
    void shouldDeliverUpdateFromScrapperPublisherToBotConsumer() {
        LinkUpdate update = new LinkUpdate()
                .id(501L)
                .url("https://github.com/org/project")
                .description("release created")
                .tgChatIds(List.of(12345L));

        updatePublisher.publish(update);

        verify(telegramBot, timeout(10_000)).execute(any(SendMessage.class));
    }
}
