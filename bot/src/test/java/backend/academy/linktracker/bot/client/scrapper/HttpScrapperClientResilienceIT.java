package backend.academy.linktracker.bot.client.scrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllScenarios;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.bot.telegram.TelegramBotService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest(
        properties = {
            "app.scrapper.base-url=${wiremock.server.baseUrl}",
            "app.http-client.connect-timeout=500ms",
            "app.http-client.read-timeout=500ms",
            "app.resilience.http.retryable-statuses=500",
            "resilience4j.retry.instances.scrapper.max-attempts=3",
            "resilience4j.retry.instances.scrapper.wait-duration=50ms",
            "resilience4j.retry.instances.scrapper.enable-exponential-backoff=false",
            "resilience4j.circuitbreaker.instances.scrapper.minimum-number-of-calls=10"
        })
@ActiveProfiles("test")
@EnableWireMock
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HttpScrapperClientResilienceIT {

    @Autowired
    private ScrapperClient scrapperClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private TelegramBotService telegramBotService;

    @BeforeEach
    void resetState() {
        resetAllRequests();
        resetAllScenarios();
        var circuitBreaker = circuitBreakerRegistry.circuitBreaker("scrapper");
        circuitBreaker.reset();
        circuitBreaker.transitionToClosedState();
    }

    @Test
    void shouldRetryRetryableScrapperStatus() {
        stubFor(post(urlEqualTo("/tg-chat/1"))
                .inScenario("retry")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second"));
        stubFor(post(urlEqualTo("/tg-chat/1"))
                .inScenario("retry")
                .whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("third"));
        stubFor(post(urlEqualTo("/tg-chat/1"))
                .inScenario("retry")
                .whenScenarioStateIs("third")
                .willReturn(aResponse().withStatus(200)));

        scrapperClient.registerChat(1L);

        verify(3, postRequestedFor(urlEqualTo("/tg-chat/1")));
    }

    @Test
    void shouldFailByTimeoutBeforeScrapperResponds() {
        stubFor(post(urlEqualTo("/tg-chat/2"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(2_000)));

        Instant startedAt = Instant.now();

        assertThatThrownBy(() -> scrapperClient.registerChat(2L)).isInstanceOf(ScrapperClientException.class);

        assertThat(Duration.between(startedAt, Instant.now())).isLessThan(Duration.ofMillis(1_500));
    }

    @Test
    void shouldNotCountBusinessExceptionsAsCircuitBreakerFailures() {
        stubFor(post(urlEqualTo("/tg-chat/3")).willReturn(aResponse().withStatus(409)));
        var circuitBreaker = circuitBreakerRegistry.circuitBreaker("scrapper");

        for (int i = 0; i < 6; i++) {
            assertThatThrownBy(() -> scrapperClient.registerChat(3L)).isInstanceOf(ChatAlreadyExistsException.class);
        }

        assertThat(circuitBreaker.getState())
                .isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }
}
