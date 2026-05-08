package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.exception.UpdateDeserializationException;
import backend.academy.linktracker.bot.service.exception.UpdateValidationException;
import backend.academy.linktracker.bot.service.idempotency.KafkaUpdateIdempotencyService;
import backend.academy.linktracker.contract.kafka.LinkUpdateAvroMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
    private final LinkUpdateAvroMapper avroMapper;
    private final KafkaUpdateIdempotencyService idempotencyService;

    @KafkaListener(topics = "${app.kafka.updates-topic}", containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, Object> record) {
        String messageId = messageId(record);
        if (idempotencyService.isProcessed(messageId)) {
            log.atInfo().addKeyValue("messageId", messageId).log("Duplicate Kafka message skipped");
            return;
        }

        LinkUpdate update = readUpdate(record.value());

        validate(update);
        processChats(messageId, update);
        idempotencyService.markProcessed(messageId);
    }

    private LinkUpdate readUpdate(Object payload) {
        try {
            if (payload instanceof GenericRecord record) {
                return avroMapper.fromRecord(record);
            }
            if (payload instanceof LinkUpdate update) {
                return update;
            }
            if (payload instanceof String text) {
                return objectMapper.readValue(text, LinkUpdate.class);
            }
            return objectMapper.convertValue(payload, LinkUpdate.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new UpdateDeserializationException("Invalid update payload", e);
        }
    }

    private void processChats(String messageId, LinkUpdate update) {
        List<Long> chatIds = update.getTgChatIds() == null ? List.of() : update.getTgChatIds();
        for (Long chatId : chatIds) {
            String deliveryId = deliveryId(messageId, chatId);
            if (idempotencyService.isProcessed(deliveryId)) {
                continue;
            }

            botUpdateService.processUpdateForChat(update, chatId);
            idempotencyService.markProcessed(deliveryId);
        }
    }

    private String messageId(ConsumerRecord<String, Object> record) {
        return record.topic() + ":" + record.partition() + ":" + record.offset();
    }

    private String deliveryId(String messageId, Long chatId) {
        return messageId + ":chat:" + chatId;
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
