package backend.academy.linktracker.scrapper.client.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record GithubIssueItem(
        @JsonProperty("title") String title,
        @JsonProperty("body") String body,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("user") GithubUser user,
        @JsonProperty("pull_request") Object pullRequest) {

    public boolean isPullRequest() {
        return pullRequest != null;
    }
}
