package backend.academy.linktracker.scrapper.service.impl;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.outbox.OutboxRepository;
import backend.academy.linktracker.scrapper.service.UpdatePublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications", name = "transport", havingValue = "HTTP")
public class BotHttpUpdatePublisher implements UpdatePublisher {

    private final BotClient botClient;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(LinkUpdate request) {
        try {
            botClient.sendUpdate(request);
        } catch (Exception e) {
            enqueueFallback(request, e);
        }
    }

    private void enqueueFallback(LinkUpdate request, Exception cause) {
        try {
            outboxRepository.enqueue(objectMapper.writeValueAsString(request));
            log.atWarn()
                    .setCause(cause)
                    .addKeyValue("updateId", request.getId())
                    .log("HTTP notification transport failed, queued update to Kafka outbox fallback");
        } catch (JsonProcessingException e) {
            cause.addSuppressed(e);
            throw new IllegalArgumentException("Failed to serialize update message for fallback outbox", cause);
        }
    }
}
