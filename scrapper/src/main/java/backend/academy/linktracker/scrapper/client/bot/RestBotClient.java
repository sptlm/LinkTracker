package backend.academy.linktracker.scrapper.client.bot;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
import backend.academy.linktracker.scrapper.api.exception.RetryableHttpStatusException;
import backend.academy.linktracker.scrapper.client.RetryableHttpStatusClassifier;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestBotClient implements BotClient {

    private final RestClient restClient;
    private final RetryableHttpStatusClassifier retryableStatusClassifier;

    @Autowired
    public RestBotClient(
            @Qualifier("botRestClient") RestClient restClient,
            RetryableHttpStatusClassifier retryableStatusClassifier) {
        this.restClient = restClient;
        this.retryableStatusClassifier = retryableStatusClassifier;
    }

    public RestBotClient(@Qualifier("botRestClient") RestClient restClient) {
        this(restClient, new RetryableHttpStatusClassifier(Set.of()));
    }

    @Override
    @Retry(name = "bot")
    @CircuitBreaker(name = "bot")
    public void sendUpdate(LinkUpdate request) {
        try {
            restClient.post().uri("/updates").body(request).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            int statusCode = e.getStatusCode().value();
            String message = "Bot request failed, status=%d".formatted(statusCode);
            if (retryableStatusClassifier.isRetryable(statusCode)) {
                throw new RetryableHttpStatusException(message, statusCode, e);
            }
            throw new ExternalServiceException(message, e);
        } catch (Exception e) {
            throw new ExternalServiceException("Bot request failed", e);
        }
    }
}
