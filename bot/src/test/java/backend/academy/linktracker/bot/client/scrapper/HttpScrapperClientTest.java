package backend.academy.linktracker.bot.client.scrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class HttpScrapperClientTest {

    @Test
    void shouldMapRetryableStatusToRetryableException() {
        WireMockServer server = new WireMockServer(0);
        try {
            server.start();
            HttpScrapperClient client = new HttpScrapperClient(
                    RestClient.builder().baseUrl(server.baseUrl()).build(),
                    new RetryableHttpStatusClassifier(Set.of(500)));
            server.stubFor(post(urlEqualTo("/tg-chat/1")).willReturn(aResponse().withStatus(500)));

            assertThrows(RetryableScrapperClientException.class, () -> client.registerChat(1L));
            server.verify(1, postRequestedFor(urlEqualTo("/tg-chat/1")));
        } finally {
            server.stop();
        }
    }

    @Test
    void shouldNotRetryNonRetryableException() {
        Retry retry = Retry.of(
                "scrapper",
                RetryConfig.custom()
                        .maxAttempts(3)
                        .waitDuration(Duration.ZERO)
                        .retryExceptions(RetryableScrapperClientException.class)
                        .build());
        AtomicInteger attempts = new AtomicInteger();
        Callable<Void> call = Retry.decorateCallable(retry, () -> {
            attempts.incrementAndGet();
            throw new ScrapperClientException("bad request");
        });

        assertThrows(ScrapperClientException.class, call::call);

        assertEquals(1, attempts.get());
    }
}
