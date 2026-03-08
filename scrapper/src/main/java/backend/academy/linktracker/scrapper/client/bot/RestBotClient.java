package backend.academy.linktracker.scrapper.client.bot;

import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestBotClient implements BotClient {

    private final RestClient restClient;

    public RestBotClient(@Qualifier("botRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void sendUpdate(LinkUpdateRequest request) {
        try {
            restClient.post()
                    .uri("/updates")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new ExternalServiceException(
                    "Bot request failed, status=%d".formatted(e.getStatusCode().value()),
                    e
            );
        } catch (Exception e) {
            throw new ExternalServiceException("Bot request failed", e);
        }
    }
}
