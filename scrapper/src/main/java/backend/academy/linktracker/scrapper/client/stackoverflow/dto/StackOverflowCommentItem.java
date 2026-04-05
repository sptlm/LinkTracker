package backend.academy.linktracker.scrapper.client.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record StackOverflowCommentItem(
        @JsonProperty("comment_id") long commentId,
        @JsonProperty("creation_date") long creationDate,
        @JsonProperty("body_markdown") String bodyMarkdown,
        @JsonProperty("owner") StackOverflowOwner owner) {
    public Instant createdAt() {
        return Instant.ofEpochSecond(creationDate);
    }
}
