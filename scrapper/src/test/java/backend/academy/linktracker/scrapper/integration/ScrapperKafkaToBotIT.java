package backend.academy.linktracker.scrapper.integration;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.configuration.KafkaNotificationsConfiguration;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.bot.properties.KafkaNotificationsProperties;
import backend.academy.linktracker.bot.service.BotUpdateService;
import backend.academy.linktracker.bot.service.KafkaUpdateConsumer;
import backend.academy.linktracker.bot.service.idempotency.KafkaUpdateIdempotencyService;
import backend.academy.linktracker.scrapper.ScrapperApplication;
import backend.academy.linktracker.scrapper.service.UpdatePublisher;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import backend.academy.linktracker.scrapper.support.KafkaTestContainerHolder;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = {ScrapperApplication.class, ScrapperKafkaToBotIT.BotKafkaConsumerTestConfiguration.class},
        properties = {
            "app.notifications.transport=KAFKA",
            "app.kafka.updates-topic=scrapper-to-bot-it",
            "app.kafka.group-id=scrapper-to-bot-group",
            "spring.flyway.enabled=false",
            "app.telegram.token=test-token"
        })
class ScrapperKafkaToBotIT {

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("app.kafka.bootstrap-servers", () -> KafkaTestContainerHolder.kafka().getBootstrapServers());
    }

    @Autowired
    private UpdatePublisher updatePublisher;

    @MockitoBean
    private BotUpdateService botUpdateService;

    @Test
    void shouldDeliverUpdateFromScrapperPublisherToBotConsumer() {
        LinkUpdate update = new LinkUpdate()
                .id(501L)
                .url(URI.create("https://github.com/org/project"))
                .description("release created")
                .tgChatIds(List.of(12345L));

        updatePublisher.publish(update);

        verify(botUpdateService, timeout(10_000))
                .processUpdate(argThat(received -> received != null
                        && received.getId().equals(update.getId())
                        && received.getUrl().equals(update.getUrl())
                        && received.getDescription().equals(update.getDescription())
                        && received.getTgChatIds().equals(update.getTgChatIds())));
    }

    @Import({KafkaUpdateConsumer.class, KafkaNotificationsConfiguration.class, KafkaUpdateIdempotencyService.class})
    @EnableConfigurationProperties(KafkaNotificationsProperties.class)
    static class BotKafkaConsumerTestConfiguration {}
}
