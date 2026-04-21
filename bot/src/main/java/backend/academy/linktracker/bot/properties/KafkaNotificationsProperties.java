package backend.academy.linktracker.bot.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    @NotBlank
    private String dlqTopic = "link-updates-dlq";

    @Min(1)
    private int maxAttempts = 3;
}
