package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotEmpty;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.resilience.http")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class ResilienceHttpProperties {

    @NotEmpty
    private Set<Integer> retryableStatuses = new LinkedHashSet<>(Set.of(500, 502, 503, 504));
}
