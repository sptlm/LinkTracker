package backend.academy.linktracker.scrapper.client.github;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.integration.AbstractPostgresIT;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.awaitility.Awaitility;
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
            "resilience4j.retry.instances.github.max-attempts=1",
            "resilience4j.circuitbreaker.instances.github.sliding-window-size=2",
            "resilience4j.circuitbreaker.instances.github.minimum-number-of-calls=2",
            "resilience4j.circuitbreaker.instances.github.failure-rate-threshold=50",
            "resilience4j.circuitbreaker.instances.github.permitted-number-of-calls-in-half-open-state=2",
            "resilience4j.circuitbreaker.instances.github.wait-duration-in-open-state=250ms",
            "resilience4j.circuitbreaker.instances.github.automatic-transition-from-open-to-half-open-enabled=true"
        })
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GithubCircuitBreakerIT extends AbstractPostgresIT {

    private static final WireMockServer GITHUB = new WireMockServer(0);

    @Autowired
    private GithubClient githubClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private GithubProperties githubProperties;

    @DynamicPropertySource
    static void githubProperties(DynamicPropertyRegistry registry) {
        registry.add("app.github.base-url", GithubCircuitBreakerIT::githubBaseUrl);
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
    void shouldOpenAndRejectCallsWithoutCallingExternalService() {
        GITHUB.stubFor(
                get(urlEqualTo("/repos/user/repo")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> githubClient.getRepository("user", "repo"));
        assertThatThrownBy(() -> githubClient.getRepository("user", "repo"));

        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("github");
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> githubClient.getRepository("user", "repo"))
                .isInstanceOf(CallNotPermittedException.class);
        GITHUB.verify(2, getRequestedFor(urlEqualTo("/repos/user/repo")));
    }

    @Test
    void shouldMoveFromHalfOpenToClosedWhenProbeCallsSucceed() {
        openCircuitBreaker();
        GITHUB.resetAll();
        GITHUB.stubFor(get(urlEqualTo("/repos/user/repo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(repositoryBody())));

        Awaitility.await().atMost(java.time.Duration.ofSeconds(2)).untilAsserted(() -> assertThat(
                        circuitBreakerRegistry.circuitBreaker("github").getState())
                .isEqualTo(CircuitBreaker.State.HALF_OPEN));

        githubClient.getRepository("user", "repo");
        githubClient.getRepository("user", "repo");

        assertThat(circuitBreakerRegistry.circuitBreaker("github").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void shouldReturnFromHalfOpenToOpenWhenProbeCallsFail() {
        openCircuitBreaker();
        GITHUB.resetAll();
        GITHUB.stubFor(
                get(urlEqualTo("/repos/user/repo")).willReturn(aResponse().withStatus(500)));

        Awaitility.await().atMost(java.time.Duration.ofSeconds(2)).untilAsserted(() -> assertThat(
                        circuitBreakerRegistry.circuitBreaker("github").getState())
                .isEqualTo(CircuitBreaker.State.HALF_OPEN));

        assertThatThrownBy(() -> githubClient.getRepository("user", "repo"));
        assertThatThrownBy(() -> githubClient.getRepository("user", "repo"));

        Awaitility.await().atMost(java.time.Duration.ofSeconds(1)).untilAsserted(() -> assertThat(
                        circuitBreakerRegistry.circuitBreaker("github").getState())
                .isEqualTo(CircuitBreaker.State.OPEN));
    }

    private void openCircuitBreaker() {
        GITHUB.stubFor(
                get(urlEqualTo("/repos/user/repo")).willReturn(aResponse().withStatus(500)));
        assertThatThrownBy(() -> githubClient.getRepository("user", "repo"));
        assertThatThrownBy(() -> githubClient.getRepository("user", "repo"));
        assertThat(circuitBreakerRegistry.circuitBreaker("github").getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    private String repositoryBody() {
        return """
                {
                  "full_name": "user/repo",
                  "html_url": "https://github.com/user/repo",
                  "pushed_at": "2026-05-17T18:00:00Z",
                  "updated_at": "2026-05-17T18:00:00Z"
                }
                """;
    }

    private static String githubBaseUrl() {
        if (!GITHUB.isRunning()) {
            GITHUB.start();
        }
        return GITHUB.baseUrl();
    }
}
