package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.http-client")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class HttpClientProperties {

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(1);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(2);
}
