package backend.academy.linktracker.scrapper.outbox;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications", name = "transport", havingValue = "KAFKA", matchIfMissing = true)
@ConditionalOnProperty(prefix = "app.kafka", name = "outbox-enabled", havingValue = "true")
public class OutboxDispatcher {

    private final OutboxRepository outboxRepository;
    private final KafkaOutboxSender kafkaOutboxSender;

    @Scheduled(fixedDelayString = "${app.kafka.outbox-dispatch-interval:1s}")
    public void dispatchPending() {
        int batchSize = 100;
        List<OutboxEvent> events = outboxRepository.findPendingBatch(batchSize);

        for (OutboxEvent event : events) {
            try {
                kafkaOutboxSender.send(event.id(), event.payload());
                outboxRepository.markSent(event.id());
            } catch (Exception e) {
                outboxRepository.incrementAttempts(event.id());
                log.atWarn()
                        .setCause(e)
                        .addKeyValue("outboxId", event.id())
                        .addKeyValue("attempts", event.attempts() + 1)
                        .log("Failed to dispatch outbox event");
            }
        }
    }
}
