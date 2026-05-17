package backend.academy.linktracker.scrapper.client.github;

import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
import backend.academy.linktracker.scrapper.api.exception.RetryableHttpStatusException;
import backend.academy.linktracker.scrapper.client.RetryableHttpStatusClassifier;
import backend.academy.linktracker.scrapper.client.github.dto.GithubIssueItem;
import backend.academy.linktracker.scrapper.client.github.dto.GithubRepositoryResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestGithubClient implements GithubClient {

    private final RestClient restClient;
    private final RetryableHttpStatusClassifier retryableStatusClassifier;

    @Autowired
    public RestGithubClient(
            @Qualifier("githubRestClient") RestClient restClient,
            RetryableHttpStatusClassifier retryableStatusClassifier) {
        this.restClient = restClient;
        this.retryableStatusClassifier = retryableStatusClassifier;
    }

    public RestGithubClient(@Qualifier("githubRestClient") RestClient restClient) {
        this(restClient, new RetryableHttpStatusClassifier(Set.of()));
    }

    @Override
    @Retry(name = "github")
    @CircuitBreaker(name = "github")
    public GithubRepositoryResponse getRepository(String owner, String repo) {
        try {
            GithubRepositoryResponse response = restClient
                    .get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .body(GithubRepositoryResponse.class);

            if (response == null) {
                throw new ExternalServiceException(
                        "GitHub returned empty response for repository %s/%s".formatted(owner, repo), null);
            }

            return response;
        } catch (RestClientResponseException e) {
            throw externalStatusException(
                    "GitHub request failed for repository %s/%s, status=%d"
                            .formatted(owner, repo, e.getStatusCode().value()),
                    e);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException(
                    "GitHub is temporarily unavailable for repository %s/%s".formatted(owner, repo), e);
        } catch (Exception e) {
            throw new ExternalServiceException("GitHub request failed for repository %s/%s".formatted(owner, repo), e);
        }
    }

    @Override
    @Retry(name = "github")
    @CircuitBreaker(name = "github")
    public List<GithubIssueItem> getLatestIssuesAndPullRequests(String owner, String repo, int limit) {
        try {
            GithubIssueItem[] items = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/issues")
                            .queryParam("state", "all")
                            .queryParam("sort", "created")
                            .queryParam("direction", "desc")
                            .queryParam("per_page", limit)
                            .build(owner, repo))
                    .retrieve()
                    .body(GithubIssueItem[].class);

            return items == null ? List.of() : Arrays.asList(items);
        } catch (RestClientResponseException e) {
            throw externalStatusException(
                    "GitHub issues request failed for repository %s/%s, status=%d"
                            .formatted(owner, repo, e.getStatusCode().value()),
                    e);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException(
                    "GitHub is temporarily unavailable for repository %s/%s".formatted(owner, repo), e);
        } catch (Exception e) {
            throw new ExternalServiceException(
                    "GitHub issues request failed for repository %s/%s".formatted(owner, repo), e);
        }
    }

    private ExternalServiceException externalStatusException(String message, RestClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        if (retryableStatusClassifier.isRetryable(statusCode)) {
            return new RetryableHttpStatusException(message, statusCode, e);
        }
        return new ExternalServiceException(message, e);
    }
}
