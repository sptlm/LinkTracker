package backend.academy.linktracker.scrapper.outbox;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.contract.kafka.LinkUpdateAvroMapper;
import backend.academy.linktracker.scrapper.properties.KafkaNotificationsProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications", name = "transport", havingValue = "KAFKA", matchIfMissing = true)
public class KafkaOutboxSender {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaNotificationsProperties kafkaProperties;
    private final ObjectMapper objectMapper;
    private final LinkUpdateAvroMapper avroMapper;

    public CompletableFuture<SendResult<String, Object>> send(long outboxId, String payload) {
        return kafkaTemplate.send(kafkaProperties.getUpdatesTopic(), String.valueOf(outboxId), kafkaPayload(payload));
    }

    private Object kafkaPayload(String payload) {
        if (!kafkaProperties.usesSchemaRegistry()) {
            return payload;
        }

        try {
            LinkUpdate update = objectMapper.readValue(payload, LinkUpdate.class);
            return avroMapper.toRecord(update);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize outbox payload for Avro publishing", e);
        }
    }
}
