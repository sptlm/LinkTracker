package backend.academy.linktracker.scrapper.outbox;

import backend.academy.linktracker.scrapper.properties.KafkaNotificationsProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.kafka",
        name = "outbox-dispatch-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OutboxDispatcher {

    private final OutboxRepository outboxRepository;
    private final KafkaOutboxSender kafkaOutboxSender;
    private final KafkaNotificationsProperties kafkaProperties;

    @Scheduled(fixedDelayString = "${app.kafka.outbox-dispatch-interval:1s}")
    public void dispatchPending() {
        int batchSize = kafkaProperties.getOutboxBatchSize();
        int maxAttempts = kafkaProperties.getOutboxMaxAttempts();
        List<OutboxEvent> events = outboxRepository.findPendingBatch(batchSize, maxAttempts);

        for (OutboxEvent event : events) {
            try {
                kafkaOutboxSender.send(event.id(), event.payload()).join();
                outboxRepository.markSent(event.id());
            } catch (Exception e) {
                int nextAttempts = event.attempts() + 1;
                outboxRepository.incrementAttempts(event.id());
                if (nextAttempts >= maxAttempts) {
                    outboxRepository.markFailed(event.id());
                }
                log.atWarn()
                        .setCause(e)
                        .addKeyValue("outboxId", event.id())
                        .addKeyValue("attempts", nextAttempts)
                        .addKeyValue("maxAttempts", maxAttempts)
                        .log("Failed to dispatch outbox event");
            }
        }
    }
}
