package backend.academy.linktracker.scrapper.client.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StackOverflowAnswersResponse(@JsonProperty("items") List<StackOverflowAnswerItem> items) {}
