package backend.academy.linktracker.scrapper.client.bot;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.contract.http.RetryableHttpStatusClassifier;
import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
import backend.academy.linktracker.scrapper.api.exception.RetryableHttpStatusException;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetrics;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestBotClient implements BotClient {

    private final RestClient restClient;
    private final RetryableHttpStatusClassifier retryableStatusClassifier;
    private final ScrapperMetrics metrics;

    @Autowired
    public RestBotClient(
            @Qualifier("botRestClient") RestClient restClient,
            RetryableHttpStatusClassifier retryableStatusClassifier,
            ScrapperMetrics metrics) {
        this.restClient = restClient;
        this.retryableStatusClassifier = retryableStatusClassifier;
        this.metrics = metrics;
    }

    public RestBotClient(@Qualifier("botRestClient") RestClient restClient) {
        this.restClient = restClient;
        this.retryableStatusClassifier = new RetryableHttpStatusClassifier(Set.of());
        this.metrics = null;
    }

    @Override
    @Retry(name = "bot")
    @CircuitBreaker(name = "bot")
    public void sendUpdate(LinkUpdate request) {
        long startedAt = System.nanoTime();
        try {
            restClient.post().uri("/updates").body(request).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException e) {
            int statusCode = e.getStatusCode().value();
            String message = "Bot request failed, status=%d".formatted(statusCode);
            if (retryableStatusClassifier.isRetryable(statusCode)) {
                throw new RetryableHttpStatusException(message, statusCode, e);
            }
            throw e;
        } catch (RestClientResponseException e) {
            int statusCode = e.getStatusCode().value();
            String message = "Bot request failed, status=%d".formatted(statusCode);
            if (retryableStatusClassifier.isRetryable(statusCode)) {
                throw new RetryableHttpStatusException(message, statusCode, e);
            }
            throw new ExternalServiceException(message, e);
        } catch (Exception e) {
            throw new ExternalServiceException("Bot request failed", e);
        } finally {
            if (metrics != null) {
                metrics.recordRequestDuration("bot_api", "updatesPost", startedAt);
            }
        }
    }
}
