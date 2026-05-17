package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
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
    @DurationMin(millis = 1)
    private Duration ttl = Duration.ofMinutes(10);

    @Valid
    @NotNull
    private ClientSide clientSide = new ClientSide();

    @Valid
    @NotNull
    private HostMapping hostMapping = new HostMapping();

    @Getter
    @Setter
    @EqualsAndHashCode
    @NoArgsConstructor
    public static class ClientSide {

        private boolean enabled = true;

        @Positive
        private int maxSize = 10_000;
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @NoArgsConstructor
    public static class HostMapping {

        private boolean enabled = true;

        @NotBlank
        private String source = "host.docker.internal";

        @NotBlank
        private String target = "localhost";
    }
}
