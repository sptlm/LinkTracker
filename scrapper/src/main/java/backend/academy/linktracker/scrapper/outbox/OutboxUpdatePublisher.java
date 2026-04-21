package backend.academy.linktracker.scrapper.outbox;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.properties.KafkaNotificationsProperties;
import backend.academy.linktracker.scrapper.properties.KafkaPayloadFormat;
import backend.academy.linktracker.scrapper.service.UpdatePublisher;
import backend.academy.linktracker.scrapper.service.codec.LinkUpdateAvroCodec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications", name = "transport", havingValue = "KAFKA", matchIfMissing = true)
@ConditionalOnProperty(prefix = "app.kafka", name = "outbox-enabled", havingValue = "true")
public class OutboxUpdatePublisher implements UpdatePublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final KafkaNotificationsProperties kafkaProperties;
    private final LinkUpdateAvroCodec avroCodec;

    @Override
    public void publish(LinkUpdate request) {
        try {
            String payload = kafkaProperties.getPayloadFormat() == KafkaPayloadFormat.AVRO
                    ? avroCodec.encodeToBase64(request)
                    : objectMapper.writeValueAsString(request);
            outboxRepository.enqueue(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize update message for outbox", e);
        }
    }
}
