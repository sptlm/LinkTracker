package backend.academy.linktracker.scrapper.client.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StackOverflowCommentsResponse(@JsonProperty("items") List<StackOverflowCommentItem> items) {}
