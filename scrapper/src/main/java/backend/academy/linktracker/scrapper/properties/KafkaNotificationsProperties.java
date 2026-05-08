package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.kafka")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class KafkaNotificationsProperties {

    @NotBlank
    private String bootstrapServers;

    @NotBlank
    private String updatesTopic = "link-updates";

    @NotNull
    @Min(1)
    private Integer updatesTopicPartitions = 3;

    @NotNull
    @Min(1)
    private Short updatesTopicReplicationFactor = 3;

    @NotNull
    @Min(1)
    private Integer updatesTopicMinInSyncReplicas = 2;

    @NotNull
    private Class<?> keySerializer = StringSerializer.class;

    @NotNull
    private Class<?> valueSerializer = StringSerializer.class;

    private String schemaRegistryUrl;

    @NotNull
    @Min(1)
    private Integer outboxMaxAttempts = 5;

    @NotNull
    @Min(1)
    private Integer outboxBatchSize = 100;

    public boolean usesSchemaRegistry() {
        return valueSerializer != null && valueSerializer.getName().startsWith("io.confluent.kafka.serializers.");
    }
}
