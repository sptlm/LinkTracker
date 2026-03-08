package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.clients")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class BotClientProperties {
    @NotBlank
    private String botBaseUrl = "http://localhost:8080";
}
