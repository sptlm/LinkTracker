package backend.academy.linktracker.bot.service.idempotency;

import backend.academy.linktracker.bot.properties.KafkaNotificationsProperties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class KafkaUpdateIdempotencyService {

    private final Set<String> processedPayloads = ConcurrentHashMap.newKeySet();
    private final AtomicInteger size = new AtomicInteger();
    private final int maxSize;

    public KafkaUpdateIdempotencyService(KafkaNotificationsProperties kafkaProperties) {
        this.maxSize = kafkaProperties.getIdempotencyCacheSize();
    }

    public boolean isProcessed(String payloadFingerprint) {
        return processedPayloads.contains(payloadFingerprint);
    }

    public void markProcessed(String payloadFingerprint) {
        if (processedPayloads.add(payloadFingerprint)) {
            int current = size.incrementAndGet();
            if (current > maxSize) {
                processedPayloads.clear();
                processedPayloads.add(payloadFingerprint);
                size.set(1);
            }
        }
    }
}
