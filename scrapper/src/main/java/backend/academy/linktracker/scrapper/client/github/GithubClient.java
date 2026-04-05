package backend.academy.linktracker.scrapper.client.github;

import backend.academy.linktracker.scrapper.client.github.dto.GithubIssueItem;
import backend.academy.linktracker.scrapper.client.github.dto.GithubRepositoryResponse;
import java.util.List;

public interface GithubClient {

    GithubRepositoryResponse getRepository(String owner, String repo);

    List<GithubIssueItem> getLatestIssuesAndPullRequests(String owner, String repo, int limit);
}
