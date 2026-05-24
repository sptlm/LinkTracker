package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.contract.kafka.LinkUpdateAvroMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RawUpdateConsumer {

    private final ObjectMapper objectMapper;
    private final LinkUpdateAvroMapper avroMapper;
    private final AiAgentUpdateProcessor updateProcessor;
    private final ProcessedUpdatePublisher updatePublisher;

    @KafkaListener(topics = "${app.kafka.raw-updates-topic}", containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, Object> record) {
        LinkUpdate update = readUpdate(record);
        if (!isValid(update)) {
            log.atWarn()
                    .addKeyValue("topic", record.topic())
                    .addKeyValue("partition", record.partition())
                    .addKeyValue("offset", record.offset())
                    .log("Invalid raw update skipped");
            return;
        }

        updateProcessor.process(update).ifPresent(updatePublisher::publish);
    }

    private LinkUpdate readUpdate(ConsumerRecord<String, Object> record) {
        try {
            Object payload = record.value();
            if (payload instanceof GenericRecord genericRecord) {
                return avroMapper.fromRecord(genericRecord);
            }
            if (payload instanceof LinkUpdate update) {
                return update;
            }
            if (payload instanceof String text) {
                return objectMapper.readValue(text, LinkUpdate.class);
            }
            return objectMapper.convertValue(payload, LinkUpdate.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.atWarn()
                    .setCause(e)
                    .addKeyValue("topic", record.topic())
                    .addKeyValue("partition", record.partition())
                    .addKeyValue("offset", record.offset())
                    .log("Failed to deserialize raw update");
            return null;
        }
    }

    private boolean isValid(LinkUpdate update) {
        return update != null
                && update.getId() != null
                && update.getUrl() != null
                && update.getDescription() != null
                && update.getTgChatIds() != null;
    }
}
