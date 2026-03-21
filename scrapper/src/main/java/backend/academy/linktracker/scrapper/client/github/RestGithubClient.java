package backend.academy.linktracker.scrapper.client.github;

import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
import backend.academy.linktracker.scrapper.client.github.dto.GithubRepositoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
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
            log.atError()
                    .setCause(e)
                    .addKeyValue("owner", owner)
                    .addKeyValue("repo", repo)
                    .addKeyValue("status", e.getStatusCode().value())
                    .log("GitHub request failed");
            throw new ExternalServiceException(
                    "GitHub request failed for repository %s/%s, status=%d"
                            .formatted(owner, repo, e.getStatusCode().value()),
                    e);
        } catch (Exception e) {
            log.atError().setCause(e).addKeyValue("owner", owner).addKeyValue("repo", repo).log("GitHub request failed");
            throw new ExternalServiceException("GitHub request failed for repository %s/%s".formatted(owner, repo), e);
        }
    }
}
