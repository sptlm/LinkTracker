package backend.academy.linktracker.scrapper.outbox;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.properties.KafkaNotificationsProperties;
import backend.academy.linktracker.scrapper.properties.KafkaPayloadFormat;
import backend.academy.linktracker.scrapper.service.codec.LinkUpdateAvroCodec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications", name = "transport", havingValue = "KAFKA", matchIfMissing = true)
public class KafkaOutboxSender {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaNotificationsProperties kafkaProperties;
    private final ObjectMapper objectMapper;
    private final LinkUpdateAvroCodec avroCodec;

    public void send(long outboxId, String payload) {
        String encodedPayload = encodePayload(payload);
        kafkaTemplate.send(kafkaProperties.getUpdatesTopic(), String.valueOf(outboxId), encodedPayload);
    }

    private String encodePayload(String payload) {
        if (kafkaProperties.getPayloadFormat() != KafkaPayloadFormat.AVRO) {
            return payload;
        }
        try {
            LinkUpdate update = objectMapper.readValue(payload, LinkUpdate.class);
            return avroCodec.encodeToBase64(update);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to decode outbox JSON payload", e);
        }
    }
}
