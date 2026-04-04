package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.persistence")
public class PersistenceProperties {

    @NotNull
    private AccessType accessType = AccessType.SQL;

    @Min(1)
    @Max(500)
    private int pollingBatchSize = 100;
}
