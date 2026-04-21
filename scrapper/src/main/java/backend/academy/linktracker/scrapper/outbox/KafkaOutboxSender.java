package backend.academy.linktracker.scrapper.outbox;

import backend.academy.linktracker.scrapper.properties.KafkaNotificationsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications", name = "transport", havingValue = "KAFKA", matchIfMissing = true)
public class KafkaOutboxSender {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaNotificationsProperties kafkaProperties;

    public void send(long outboxId, String payload) {
        kafkaTemplate.send(kafkaProperties.getUpdatesTopic(), String.valueOf(outboxId), payload);
    }
}
