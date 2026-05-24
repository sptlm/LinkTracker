package backend.academy.linktracker.ai.properties;

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
    private String rawUpdatesTopic = "link.raw-updates";

    @NotBlank
    private String processedUpdatesTopic = "link.processed-updates";

    @NotBlank
    private String groupId = "ai-agent-updates-consumer";

    @NotNull
    @Min(1)
    private Integer topicsPartitions = 3;

    @NotNull
    @Min(1)
    private Short topicsReplicationFactor = 3;

    @NotNull
    private Class<?> keySerializer = StringSerializer.class;

    @NotNull
    private Class<?> valueSerializer = StringSerializer.class;

    @NotNull
    private Class<?> keyDeserializer = StringDeserializer.class;

    @NotNull
    private Class<?> valueDeserializer = StringDeserializer.class;

    private String schemaRegistryUrl;

    public boolean usesSchemaRegistry() {
        return isAvroClass(valueSerializer) || isAvroClass(valueDeserializer);
    }

    private static boolean isAvroClass(Class<?> serializerClass) {
        return serializerClass != null && serializerClass.getName().startsWith("io.confluent.kafka.serializers.");
    }
}
