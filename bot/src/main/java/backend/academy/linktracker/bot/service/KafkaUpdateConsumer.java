package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.bot.properties.KafkaNotificationsProperties;
import backend.academy.linktracker.bot.properties.KafkaPayloadFormat;
import backend.academy.linktracker.bot.service.exception.UpdateDeserializationException;
import backend.academy.linktracker.bot.service.exception.UpdateValidationException;
import backend.academy.linktracker.bot.service.idempotency.KafkaUpdateIdempotencyService;
import backend.academy.linktracker.contract.kafka.LinkUpdateAvroCodec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications", name = "transport", havingValue = "KAFKA", matchIfMissing = true)
public class KafkaUpdateConsumer {

    private final BotUpdateService botUpdateService;
    private final ObjectMapper objectMapper;
    private final KafkaNotificationsProperties kafkaProperties;
    private final LinkUpdateAvroCodec avroCodec;
    private final KafkaUpdateIdempotencyService idempotencyService;

    @KafkaListener(topics = "${app.kafka.updates-topic}", containerFactory = "kafkaListenerContainerFactory")
    public void consume(String payload) {
        String fingerprint = fingerprint(payload);
        if (idempotencyService.isProcessed(fingerprint)) {
            log.atInfo().addKeyValue("fingerprint", fingerprint).log("Duplicate Kafka update payload skipped");
            return;
        }

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
        idempotencyService.markProcessed(fingerprint);
    }

    private String fingerprint(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
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
