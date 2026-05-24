package backend.academy.linktracker.scrapper.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllScenarios;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.integration.AbstractPostgresIT;
import backend.academy.linktracker.scrapper.outbox.OutboxRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.wiremock.spring.EnableWireMock;

@TestPropertySource(
        properties = {
            "app.notifications.transport=HTTP",
            "app.clients.bot-base-url=${wiremock.server.baseUrl}",
            "app.resilience.http.retryable-statuses=500",
            "resilience4j.retry.instances.bot.max-attempts=3",
            "resilience4j.retry.instances.bot.wait-duration=10ms",
            "resilience4j.circuitbreaker.instances.bot.minimum-number-of-calls=10",
            "spring.task.scheduling.enabled=false",
            "app.kafka.outbox-dispatch-enabled=false"
        })
@EnableWireMock
class HttpNotificationFallbackIT extends AbstractPostgresIT {

    @Autowired
    private UpdatePublisher updatePublisher;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetResilienceAndWireMockState() {
        resetAllRequests();
        resetAllScenarios();
        var circuitBreaker = circuitBreakerRegistry.circuitBreaker("bot");
        circuitBreaker.reset();
        circuitBreaker.transitionToClosedState();
    }

    @Test
    void shouldQueueUpdateToKafkaOutboxWhenPrimaryHttpTransportFails() {
        stubFor(post(urlEqualTo("/updates")).willReturn(aResponse().withStatus(500)));
        LinkUpdate update = new LinkUpdate()
                .id(77L)
                .url(URI.create("https://github.com/example/repo"))
                .description("fallback")
                .tgChatIds(List.of(123L));

        updatePublisher.publish(update);

        verify(3, postRequestedFor(urlEqualTo("/updates")));
        var pending = outboxRepository.findPendingBatch(10, 3);
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().payload()).contains("\"id\":77");
    }
}
