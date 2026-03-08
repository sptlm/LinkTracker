package backend.academy.linktracker.scrapper.client.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record StackOverflowQuestionItem(
        @JsonProperty("question_id") long questionId,
        @JsonProperty("title") String title,
        @JsonProperty("link") String link,
        @JsonProperty("answer_count") int answerCount,
        @JsonProperty("is_answered") boolean answered,
        @JsonProperty("last_activity_date") long lastActivityDate
) {
    public Instant lastActivityAt() {
        return Instant.ofEpochSecond(lastActivityDate);
    }
}
