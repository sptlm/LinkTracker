package backend.academy.linktracker.scrapper.client.stackoverflow;

import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowQuestionItem;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowQuestionsResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class RestStackOverflowClient implements StackOverflowClient {

    private final RestClient restClient;

    public RestStackOverflowClient(@Qualifier("stackOverflowRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public StackOverflowQuestionItem getQuestion(long questionId) {
        try {
            StackOverflowQuestionsResponse response = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/questions/{id}")
                    .queryParam("site", "stackoverflow")
                    .build(questionId))
                .retrieve()
                .body(StackOverflowQuestionsResponse.class);

            List<StackOverflowQuestionItem> items = response != null ? response.items() : List.of();
            if (items == null || items.isEmpty()) {
                throw new ExternalServiceException(
                    "StackOverflow returned empty response for question " + questionId, null);
            }

            return items.getFirst();
        } catch (RestClientResponseException e) {
            log.atError()
                .setCause(e)
                .addKeyValue("questionId", questionId)
                .addKeyValue("status", e.getStatusCode().value())
                .log("StackOverflow request failed");
            throw new ExternalServiceException(
                "StackOverflow request failed for question %d, status=%d"
                    .formatted(questionId, e.getStatusCode().value()),
                e);
        } catch (Exception e) {
            log.atError().setCause(e).addKeyValue("questionId", questionId).log("StackOverflow request failed");
            throw new ExternalServiceException("StackOverflow request failed for question " + questionId, e);
        }
    }
}
