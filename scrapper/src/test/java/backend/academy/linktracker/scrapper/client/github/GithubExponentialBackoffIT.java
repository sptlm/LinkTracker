package backend.academy.linktracker.scrapper.client.github;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
import backend.academy.linktracker.scrapper.integration.AbstractPostgresIT;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Comparator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "spring.task.scheduling.enabled=false",
            "app.resilience.http.retryable-statuses=500",
            "resilience4j.retry.instances.github.max-attempts=3",
            "resilience4j.retry.instances.github.wait-duration=100ms",
            "resilience4j.retry.instances.github.enable-exponential-backoff=true",
            "resilience4j.retry.instances.github.exponential-backoff-multiplier=2",
            "resilience4j.circuitbreaker.instances.github.minimum-number-of-calls=10"
        })
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GithubExponentialBackoffIT extends AbstractPostgresIT {

    private static final WireMockServer GITHUB = new WireMockServer(0);

    @Autowired
    private GithubClient githubClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private GithubProperties githubProperties;

    @DynamicPropertySource
    static void githubProperties(DynamicPropertyRegistry registry) {
        registry.add("app.github.base-url", GithubExponentialBackoffIT::githubBaseUrl);
    }

    @BeforeAll
    static void startWireMock() {
        if (!GITHUB.isRunning()) {
            GITHUB.start();
        }
    }

    @AfterAll
    static void stopWireMock() {
        GITHUB.stop();
    }

    @BeforeEach
    void resetResilienceState() {
        assertThat(githubProperties.getBaseUrl()).isEqualTo(GITHUB.baseUrl());
        GITHUB.resetAll();
        var circuitBreaker = circuitBreakerRegistry.circuitBreaker("github");
        circuitBreaker.reset();
        circuitBreaker.transitionToClosedState();
    }

    @Test
    void shouldSupportExponentialBackoffAsAlternativeStrategy() {
        GITHUB.stubFor(
                get(urlEqualTo("/repos/user/repo")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> githubClient.getRepository("user", "repo"))
                .isInstanceOf(ExternalServiceException.class);

        var events = GITHUB.getAllServeEvents().stream()
                .sorted(Comparator.comparing(event -> event.getRequest().getLoggedDate()))
                .toList();
        assertThat(events).hasSize(3);

        long firstGap = events.get(1).getRequest().getLoggedDate().getTime()
                - events.get(0).getRequest().getLoggedDate().getTime();
        long secondGap = events.get(2).getRequest().getLoggedDate().getTime()
                - events.get(1).getRequest().getLoggedDate().getTime();

        assertThat(firstGap).isGreaterThanOrEqualTo(80);
        assertThat(secondGap).isGreaterThan(firstGap + 50);
    }

    private static String githubBaseUrl() {
        if (!GITHUB.isRunning()) {
            GITHUB.start();
        }
        return GITHUB.baseUrl();
    }
}
