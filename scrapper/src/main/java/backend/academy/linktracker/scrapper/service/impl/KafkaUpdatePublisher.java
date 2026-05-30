package backend.academy.linktracker.scrapper.service.impl;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.contract.kafka.LinkUpdateAvroMapper;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.properties.KafkaNotificationsProperties;
import backend.academy.linktracker.scrapper.service.UpdatePublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications", name = "transport", havingValue = "KAFKA", matchIfMissing = true)
@ConditionalOnProperty(prefix = "app.kafka", name = "direct-publisher-enabled", havingValue = "true")
public class KafkaUpdatePublisher implements UpdatePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaNotificationsProperties kafkaProperties;
    private final ObjectMapper objectMapper;
    private final LinkUpdateAvroMapper avroMapper;
    private final ScrapperMetrics metrics;

    @Override
    public void publish(LinkUpdate request) {
        long startedAt = System.nanoTime();
        try {
            Object payload = kafkaProperties.usesSchemaRegistry()
                    ? avroMapper.toRecord(request)
                    : objectMapper.writeValueAsString(request);
            kafkaTemplate
                    .send(kafkaProperties.getUpdatesTopic(), String.valueOf(request.getId()), payload)
                    .whenComplete((result, error) ->
                            metrics.recordRequestDuration("kafka", kafkaProperties.getUpdatesTopic(), startedAt));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize update message", e);
        }
    }
}
