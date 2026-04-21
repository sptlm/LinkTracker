package backend.academy.linktracker.bot.service.idempotency;

import backend.academy.linktracker.bot.properties.KafkaNotificationsProperties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class KafkaUpdateIdempotencyService {

    private final Set<String> processedMessageIds = ConcurrentHashMap.newKeySet();
    private final AtomicInteger size = new AtomicInteger();
    private final int maxSize;

    public KafkaUpdateIdempotencyService(KafkaNotificationsProperties kafkaProperties) {
        this.maxSize = kafkaProperties.getIdempotencyCacheSize();
    }

    public boolean isProcessed(String messageId) {
        return processedMessageIds.contains(messageId);
    }

    public void markProcessed(String messageId) {
        if (processedMessageIds.add(messageId)) {
            int current = size.incrementAndGet();
            if (current > maxSize) {
                processedMessageIds.clear();
                processedMessageIds.add(messageId);
                size.set(1);
            }
        }
    }
}
