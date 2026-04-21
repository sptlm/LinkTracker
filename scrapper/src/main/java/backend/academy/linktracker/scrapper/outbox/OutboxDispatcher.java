package backend.academy.linktracker.scrapper.outbox;

import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
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
    private final Map<Long, Long> nextRetryEpochMsByOutboxId = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${app.kafka.outbox-dispatch-interval:1s}")
    public void dispatchPending() {
        int batchSize = 100;
        List<OutboxEvent> events = outboxRepository.findPendingBatch(batchSize);
        long nowEpochMs = System.currentTimeMillis();

        for (OutboxEvent event : events) {
            if (!canAttemptNow(event.id(), nowEpochMs)) {
                continue;
            }
            try {
                kafkaOutboxSender.send(event.id(), event.payload());
                outboxRepository.markSent(event.id());
                nextRetryEpochMsByOutboxId.remove(event.id());
            } catch (Exception e) {
                outboxRepository.incrementAttempts(event.id());
                long nextRetryInMs = calculateRetryDelayMs(event.attempts() + 1);
                nextRetryEpochMsByOutboxId.put(event.id(), nowEpochMs + nextRetryInMs);

                if (isTransientConnectivityIssue(e)) {
                    log.atWarn()
                            .addKeyValue("outboxId", event.id())
                            .addKeyValue("attempts", event.attempts() + 1)
                            .addKeyValue("retryInMs", nextRetryInMs)
                            .log("Failed to dispatch outbox event due to temporary connectivity issue");
                    continue;
                }
                log.atWarn()
                        .setCause(e)
                        .addKeyValue("outboxId", event.id())
                        .addKeyValue("attempts", event.attempts() + 1)
                        .addKeyValue("retryInMs", nextRetryInMs)
                        .log("Failed to dispatch outbox event");
            }
        }
    }

    private boolean canAttemptNow(long outboxId, long nowEpochMs) {
        Long nextRetryAt = nextRetryEpochMsByOutboxId.get(outboxId);
        return nextRetryAt == null || nowEpochMs >= nextRetryAt;
    }

    private long calculateRetryDelayMs(int attempts) {
        int normalizedAttempts = Math.max(1, Math.min(attempts, 6));
        long seconds = 1L << (normalizedAttempts - 1);
        return TimeUnit.SECONDS.toMillis(seconds);
    }

    private boolean isTransientConnectivityIssue(@NonNull Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ConnectException || current instanceof ClosedChannelException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
