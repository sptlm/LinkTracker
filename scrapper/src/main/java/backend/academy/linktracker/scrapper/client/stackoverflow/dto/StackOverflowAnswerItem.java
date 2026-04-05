package backend.academy.linktracker.scrapper.client.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record StackOverflowAnswerItem(
        @JsonProperty("answer_id") long answerId,
        @JsonProperty("creation_date") long creationDate,
        @JsonProperty("body_markdown") String bodyMarkdown,
        @JsonProperty("owner") StackOverflowOwner owner) {
    public Instant createdAt() {
        return Instant.ofEpochSecond(creationDate);
    }
}
