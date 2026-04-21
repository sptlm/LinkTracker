package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
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

    @KafkaListener(topics = "${app.kafka.updates-topic}", containerFactory = "kafkaListenerContainerFactory")
    public void consume(String payload) {
        try {
            LinkUpdate update = objectMapper.readValue(payload, LinkUpdate.class);
            botUpdateService.processUpdate(update);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid update payload", e);
        }
    }
}
