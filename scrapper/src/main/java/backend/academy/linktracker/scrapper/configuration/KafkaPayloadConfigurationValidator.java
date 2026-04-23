package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.KafkaNotificationsProperties;
import backend.academy.linktracker.scrapper.properties.KafkaPayloadFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications", name = "transport", havingValue = "KAFKA", matchIfMissing = true)
public class KafkaPayloadConfigurationValidator {

    private final KafkaNotificationsProperties properties;

    @jakarta.annotation.PostConstruct
    void validate() {
        if (properties.getPayloadFormat() == KafkaPayloadFormat.AVRO
                && (properties.getSchemaRegistryUrl() == null
                        || properties.getSchemaRegistryUrl().isBlank())) {
            throw new IllegalStateException(
                    "app.kafka.schema-registry-url must be set when app.kafka.payload-format=AVRO");
        }
    }
}
