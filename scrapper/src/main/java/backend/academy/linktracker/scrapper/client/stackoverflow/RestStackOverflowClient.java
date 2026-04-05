package backend.academy.linktracker.scrapper.client.stackoverflow;

import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowAnswerItem;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowAnswersResponse;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowCommentItem;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowCommentsResponse;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowQuestionItem;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowQuestionsResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

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
            throw new ExternalServiceException(
                    "StackOverflow request failed for question %d, status=%d"
                            .formatted(questionId, e.getStatusCode().value()),
                    e);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("StackOverflow is temporarily unavailable for question " + questionId, e);
        } catch (Exception e) {
            throw new ExternalServiceException("StackOverflow request failed for question " + questionId, e);
        }
    }

    @Override
    public List<StackOverflowAnswerItem> getLatestAnswers(long questionId, int limit) {
        try {
            StackOverflowAnswersResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{id}/answers")
                            .queryParam("site", "stackoverflow")
                            .queryParam("order", "desc")
                            .queryParam("sort", "creation")
                            .queryParam("filter", "withbody")
                            .queryParam("pagesize", limit)
                            .build(questionId))
                    .retrieve()
                    .body(StackOverflowAnswersResponse.class);

            return response == null || response.items() == null ? List.of() : response.items();
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("StackOverflow is temporarily unavailable for question " + questionId, e);
        } catch (Exception e) {
            throw new ExternalServiceException("StackOverflow answers request failed for question " + questionId, e);
        }
    }

    @Override
    public List<StackOverflowCommentItem> getLatestComments(long questionId, int limit) {
        try {
            StackOverflowCommentsResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{id}/comments")
                            .queryParam("site", "stackoverflow")
                            .queryParam("order", "desc")
                            .queryParam("sort", "creation")
                            .queryParam("filter", "withbody")
                            .queryParam("pagesize", limit)
                            .build(questionId))
                    .retrieve()
                    .body(StackOverflowCommentsResponse.class);

            return response == null || response.items() == null ? List.of() : response.items();
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("StackOverflow is temporarily unavailable for question " + questionId, e);
        } catch (Exception e) {
            throw new ExternalServiceException("StackOverflow comments request failed for question " + questionId, e);
        }
    }
}
