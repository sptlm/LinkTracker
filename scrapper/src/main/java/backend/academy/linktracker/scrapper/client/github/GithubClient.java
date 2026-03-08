package backend.academy.linktracker.scrapper.client.github;

import backend.academy.linktracker.scrapper.client.github.dto.GithubRepositoryResponse;

public interface GithubClient {

    GithubRepositoryResponse getRepository(String owner, String repo);
}
