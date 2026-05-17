package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.rate-limit")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class RateLimitProperties {

    private boolean enabled = true;

    @Min(1)
    private long capacity = 60;

    @Min(1)
    private long refillTokens = 60;

    @NotNull
    private Duration refillPeriod = Duration.ofMinutes(1);

    @NotNull
    private Duration cacheExpireAfterAccess = Duration.ofMinutes(10);
}
