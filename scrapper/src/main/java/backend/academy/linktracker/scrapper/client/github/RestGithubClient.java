package backend.academy.linktracker.scrapper.client.github;

import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
import backend.academy.linktracker.scrapper.client.github.dto.GithubIssueItem;
import backend.academy.linktracker.scrapper.client.github.dto.GithubRepositoryResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestGithubClient implements GithubClient {

    private final RestClient restClient;

    public RestGithubClient(@Qualifier("githubRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
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
            throw new ExternalServiceException(
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
    public List<GithubIssueItem> getLatestIssuesAndPullRequests(String owner, String repo, int limit) {
        try {
            GithubIssueItem[] items = restClient
                    .get()
                    .uri(
                            uriBuilder -> uriBuilder
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
            throw new ExternalServiceException(
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
}
