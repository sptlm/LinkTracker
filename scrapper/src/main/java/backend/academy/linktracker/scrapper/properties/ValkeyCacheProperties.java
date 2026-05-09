package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.valkey.cache")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class ValkeyCacheProperties {

    private boolean enabled = true;

    @NotNull
    private Duration ttl = Duration.ofMinutes(10);

    @NotNull
    private ClientSide clientSide = new ClientSide();

    @Getter
    @Setter
    @EqualsAndHashCode
    @NoArgsConstructor
    public static class ClientSide {

        private boolean enabled = true;

        private int maxSize = 10_000;
    }
}
