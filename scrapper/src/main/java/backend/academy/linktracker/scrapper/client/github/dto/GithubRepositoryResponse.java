package backend.academy.linktracker.scrapper.client.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record GithubRepositoryResponse(
        @JsonProperty("full_name") String fullName,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("pushed_at") Instant pushedAt,
        @JsonProperty("updated_at") Instant updatedAt) {}
