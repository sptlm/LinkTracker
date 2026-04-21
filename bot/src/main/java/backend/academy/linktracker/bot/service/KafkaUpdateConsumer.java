package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.bot.properties.KafkaNotificationsProperties;
import backend.academy.linktracker.bot.properties.KafkaPayloadFormat;
import backend.academy.linktracker.bot.service.codec.LinkUpdateAvroCodec;
import backend.academy.linktracker.bot.service.exception.UpdateDeserializationException;
import backend.academy.linktracker.bot.service.exception.UpdateValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications", name = "transport", havingValue = "KAFKA", matchIfMissing = true)
public class KafkaUpdateConsumer {

    private final BotUpdateService botUpdateService;
    private final ObjectMapper objectMapper;
    private final KafkaNotificationsProperties kafkaProperties;
    private final LinkUpdateAvroCodec avroCodec;

    @KafkaListener(topics = "${app.kafka.updates-topic}", containerFactory = "kafkaListenerContainerFactory")
    public void consume(String payload) {
        LinkUpdate update;
        try {
            update = kafkaProperties.getPayloadFormat() == KafkaPayloadFormat.AVRO
                    ? avroCodec.decodeFromBase64(payload)
                    : objectMapper.readValue(payload, LinkUpdate.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new UpdateDeserializationException("Invalid update payload", e);
        }

        validate(update);
        botUpdateService.processUpdate(update);
    }

    private void validate(LinkUpdate update) {
        if (update == null
                || update.getId() == null
                || update.getUrl() == null
                || update.getDescription() == null
                || update.getDescription().isBlank()) {
            throw new UpdateValidationException("Update payload validation failed");
        }
    }
}
