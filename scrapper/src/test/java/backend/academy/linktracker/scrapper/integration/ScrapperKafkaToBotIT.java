package backend.academy.linktracker.scrapper.integration;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.ScrapperApplication;
import backend.academy.linktracker.scrapper.service.UpdatePublisher;
import backend.academy.linktracker.scrapper.support.KafkaTestContainerHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = {ScrapperApplication.class, ScrapperKafkaToBotIT.BotKafkaConsumerTestConfiguration.class},
        properties = {
            "app.notifications.transport=KAFKA",
            "app.kafka.updates-topic=scrapper-to-bot-it",
            "app.kafka.group-id=scrapper-to-bot-group",
            "spring.flyway.enabled=false"
        })
class ScrapperKafkaToBotIT {

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("app.kafka.bootstrap-servers", () -> KafkaTestContainerHolder.kafka().getBootstrapServers());
    }

    @Autowired
    private UpdatePublisher updatePublisher;

    @MockitoBean
    private UpdateSink updateSink;

    @Test
    void shouldDeliverUpdateFromScrapperPublisherToBotConsumer() {
        LinkUpdate update = new LinkUpdate()
                .id(501L)
                .url(URI.create("https://github.com/org/project"))
                .description("release created")
                .tgChatIds(List.of(12345L));

        updatePublisher.publish(update);

        verify(updateSink, timeout(10_000))
                .process(argThat(received -> received != null
                        && received.getId().equals(update.getId())
                        && received.getUrl().equals(update.getUrl())
                        && received.getDescription().equals(update.getDescription())
                        && received.getTgChatIds().equals(update.getTgChatIds())));
    }

    interface UpdateSink {
        void process(LinkUpdate update);
    }

    @EnableKafka
    @Import(TestKafkaListener.class)
    static class BotKafkaConsumerTestConfiguration {

        @Bean
        ConsumerFactory<String, String> consumerFactory() {
            Map<String, Object> config = new HashMap<>();
            config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestContainerHolder.kafka().getBootstrapServers());
            config.put(ConsumerConfig.GROUP_ID_CONFIG, "scrapper-to-bot-group");
            config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
                ConsumerFactory<String, String> consumerFactory) {
            ConcurrentKafkaListenerContainerFactory<String, String> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory);
            return factory;
        }

        @Bean
        TestKafkaListener testKafkaListener(ObjectMapper objectMapper, UpdateSink updateSink) {
            return new TestKafkaListener(objectMapper, updateSink);
        }
    }

    static class TestKafkaListener {

        private final ObjectMapper objectMapper;
        private final UpdateSink updateSink;

        TestKafkaListener(ObjectMapper objectMapper, UpdateSink updateSink) {
            this.objectMapper = objectMapper;
            this.updateSink = updateSink;
        }

        @KafkaListener(topics = "scrapper-to-bot-it", containerFactory = "kafkaListenerContainerFactory")
        void consume(String payload) throws Exception {
            updateSink.process(objectMapper.readValue(payload, LinkUpdate.class));
        }
    }
}
