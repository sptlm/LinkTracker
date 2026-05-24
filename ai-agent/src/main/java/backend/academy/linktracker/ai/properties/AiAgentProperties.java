package backend.academy.linktracker.ai.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "ai-agent")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class AiAgentProperties {

    @Valid
    @NotNull
    private Filtering filtering = new Filtering();

    @Valid
    @NotNull
    private Summarization summarization = new Summarization();

    @Getter
    @Setter
    @EqualsAndHashCode
    @NoArgsConstructor
    public static class Filtering {

        @NotNull
        private List<String> stopWords = new ArrayList<>();

        @NotNull
        private List<String> excludedAuthors = new ArrayList<>();

        @Min(0)
        private int minLength = 0;
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @NoArgsConstructor
    public static class Summarization {

        @Min(1)
        private int threshold = 500;

        @NotNull
        private Provider provider = Provider.API;

        @Valid
        @NotNull
        private Api api = new Api();
    }

    public enum Provider {
        API,
        STUB
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @NoArgsConstructor
    public static class Api {

        private String baseUrl;

        private String token;

        private String model;

        @NotNull
        private Duration timeout = Duration.ofSeconds(5);
    }
}
