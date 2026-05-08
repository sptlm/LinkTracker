package backend.academy.linktracker.bot.service.idempotency;

import backend.academy.linktracker.bot.properties.KafkaNotificationsProperties;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class KafkaUpdateIdempotencyService {

    private final Set<String> processedMessageIds = new LinkedHashSet<>();
    private final int maxSize;

    public KafkaUpdateIdempotencyService(KafkaNotificationsProperties kafkaProperties) {
        this.maxSize = kafkaProperties.getIdempotencyCacheSize();
    }

    public synchronized boolean isProcessed(String messageId) {
        return processedMessageIds.contains(messageId);
    }

    public synchronized void markProcessed(String messageId) {
        if (processedMessageIds.add(messageId)) {
            while (processedMessageIds.size() > maxSize) {
                processedMessageIds.remove(processedMessageIds.iterator().next());
            }
        }
    }
}
