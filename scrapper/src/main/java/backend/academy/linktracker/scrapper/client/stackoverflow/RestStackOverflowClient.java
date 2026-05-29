package backend.academy.linktracker.scrapper.client.stackoverflow;

import backend.academy.linktracker.contract.http.RetryableHttpStatusClassifier;
import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
import backend.academy.linktracker.scrapper.api.exception.RetryableHttpStatusException;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowAnswerItem;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowAnswersResponse;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowCommentItem;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowCommentsResponse;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowQuestionItem;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowQuestionsResponse;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetrics;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestStackOverflowClient implements StackOverflowClient {

    private final RestClient restClient;
    private final RetryableHttpStatusClassifier retryableStatusClassifier;
    private final ScrapperMetrics metrics;

    @Autowired
    public RestStackOverflowClient(
            @Qualifier("stackOverflowRestClient") RestClient restClient,
            RetryableHttpStatusClassifier retryableStatusClassifier,
            ScrapperMetrics metrics) {
        this.restClient = restClient;
        this.retryableStatusClassifier = retryableStatusClassifier;
        this.metrics = metrics;
    }

    public RestStackOverflowClient(@Qualifier("stackOverflowRestClient") RestClient restClient) {
        this.restClient = restClient;
        this.retryableStatusClassifier = new RetryableHttpStatusClassifier(Set.of());
        this.metrics = null;
    }

    @Override
    @Retry(name = "stackoverflow")
    @CircuitBreaker(name = "stackoverflow")
    public StackOverflowQuestionItem getQuestion(long questionId) {
        long startedAt = System.nanoTime();
        try {
            StackOverflowQuestionsResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{id}")
                            .queryParam("site", "stackoverflow")
                            .build(questionId))
                    .retrieve()
                    .body(StackOverflowQuestionsResponse.class);

            List<StackOverflowQuestionItem> items = response != null ? response.items() : List.of();
            if (items == null || items.isEmpty()) {
                throw new ExternalServiceException(
                        "StackOverflow returned empty response for question " + questionId, null);
            }

            return items.getFirst();
        } catch (HttpClientErrorException e) {
            throw clientStatusException(questionId, e);
        } catch (RestClientResponseException e) {
            throw externalStatusException(
                    "StackOverflow request failed for question %d, status=%d"
                            .formatted(questionId, e.getStatusCode().value()),
                    e);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException(
                    "StackOverflow is temporarily unavailable for question " + questionId, e);
        } catch (Exception e) {
            throw new ExternalServiceException("StackOverflow request failed for question " + questionId, e);
        } finally {
            recordExternalSourceDuration(startedAt);
        }
    }

    @Override
    @Retry(name = "stackoverflow")
    @CircuitBreaker(name = "stackoverflow")
    public List<StackOverflowAnswerItem> getLatestAnswers(long questionId, int limit) {
        long startedAt = System.nanoTime();
        try {
            StackOverflowAnswersResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{id}/answers")
                            .queryParam("site", "stackoverflow")
                            .queryParam("order", "desc")
                            .queryParam("sort", "creation")
                            .queryParam("filter", "withbody")
                            .queryParam("pagesize", limit)
                            .build(questionId))
                    .retrieve()
                    .body(StackOverflowAnswersResponse.class);

            return response == null || response.items() == null ? List.of() : response.items();
        } catch (HttpClientErrorException e) {
            throw clientStatusException(questionId, e);
        } catch (RestClientResponseException e) {
            throw externalStatusException(
                    "StackOverflow answers request failed for question %d, status=%d"
                            .formatted(questionId, e.getStatusCode().value()),
                    e);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException(
                    "StackOverflow is temporarily unavailable for question " + questionId, e);
        } catch (Exception e) {
            throw new ExternalServiceException("StackOverflow answers request failed for question " + questionId, e);
        } finally {
            recordExternalSourceDuration(startedAt);
        }
    }

    @Override
    @Retry(name = "stackoverflow")
    @CircuitBreaker(name = "stackoverflow")
    public List<StackOverflowCommentItem> getLatestComments(long questionId, int limit) {
        long startedAt = System.nanoTime();
        try {
            StackOverflowCommentsResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{id}/comments")
                            .queryParam("site", "stackoverflow")
                            .queryParam("order", "desc")
                            .queryParam("sort", "creation")
                            .queryParam("filter", "withbody")
                            .queryParam("pagesize", limit)
                            .build(questionId))
                    .retrieve()
                    .body(StackOverflowCommentsResponse.class);

            return response == null || response.items() == null ? List.of() : response.items();
        } catch (HttpClientErrorException e) {
            throw clientStatusException(questionId, e);
        } catch (RestClientResponseException e) {
            throw externalStatusException(
                    "StackOverflow comments request failed for question %d, status=%d"
                            .formatted(questionId, e.getStatusCode().value()),
                    e);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException(
                    "StackOverflow is temporarily unavailable for question " + questionId, e);
        } catch (Exception e) {
            throw new ExternalServiceException("StackOverflow comments request failed for question " + questionId, e);
        } finally {
            recordExternalSourceDuration(startedAt);
        }
    }

    private void recordExternalSourceDuration(long startedAt) {
        if (metrics != null) {
            metrics.recordRequestDuration("external_source", "stackoverflow", startedAt);
        }
    }

    private ExternalServiceException externalStatusException(String message, RestClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        if (retryableStatusClassifier.isRetryable(statusCode)) {
            return new RetryableHttpStatusException(message, statusCode, e);
        }
        return new ExternalServiceException(message, e);
    }

    private RuntimeException clientStatusException(long questionId, HttpClientErrorException e) {
        int statusCode = e.getStatusCode().value();
        if (retryableStatusClassifier.isRetryable(statusCode)) {
            return new RetryableHttpStatusException(
                    "StackOverflow request failed for question %d, status=%d".formatted(questionId, statusCode),
                    statusCode,
                    e);
        }
        return e;
    }
}
