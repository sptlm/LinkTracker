package backend.academy.linktracker.bot.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.kafka.common.serialization.StringDeserializer;
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

    @NotBlank
    private String groupId = "bot-updates-consumer";

    @NotNull
    private Class<?> keyDeserializer = StringDeserializer.class;

    @NotNull
    private Class<?> valueDeserializer = StringDeserializer.class;

    @NotNull
    private Class<?> dlqKeySerializer = StringSerializer.class;

    @NotNull
    private Class<?> dlqValueSerializer = StringSerializer.class;

    private String schemaRegistryUrl;

    @NotBlank
    private String dlqTopic = "link-updates-dlq";

    @NotNull
    @Min(1)
    private Integer dlqTopicPartitions = 3;

    @NotNull
    @Min(1)
    private Short dlqTopicReplicationFactor = 3;

    @Min(1)
    private int maxAttempts = 3;

    @Min(1)
    private int idempotencyCacheSize = 10000;

    public boolean usesSchemaRegistry() {
        return isAvroClass(valueDeserializer) || isAvroClass(dlqValueSerializer);
    }

    private static boolean isAvroClass(Class<?> serializerClass) {
        return serializerClass != null && serializerClass.getName().startsWith("io.confluent.kafka.serializers.");
    }
}
