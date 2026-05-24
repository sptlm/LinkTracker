package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.properties.KafkaNotificationsProperties;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.contract.kafka.LinkUpdateAvroMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessedUpdatePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaNotificationsProperties kafkaProperties;
    private final ObjectMapper objectMapper;
    private final LinkUpdateAvroMapper avroMapper;

    public void publish(LinkUpdate update) {
        try {
            Object payload = kafkaProperties.usesSchemaRegistry()
                    ? avroMapper.toRecord(update)
                    : objectMapper.writeValueAsString(update);
            kafkaTemplate
                    .send(kafkaProperties.getProcessedUpdatesTopic(), String.valueOf(update.getId()), payload)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.atWarn()
                                    .setCause(exception)
                                    .addKeyValue("updateId", update.getId())
                                    .addKeyValue("topic", kafkaProperties.getProcessedUpdatesTopic())
                                    .log("Failed to publish processed update");
                        }
                    });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize processed update", e);
        }
    }
}
