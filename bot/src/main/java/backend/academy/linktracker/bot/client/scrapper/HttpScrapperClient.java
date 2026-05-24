package backend.academy.linktracker.bot.client.scrapper;

import backend.academy.linktracker.contract.http.RetryableHttpStatusClassifier;
import backend.academy.linktracker.scrapper.generated.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.generated.dto.LinkResponse;
import backend.academy.linktracker.scrapper.generated.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.generated.dto.RemoveLinkRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class HttpScrapperClient implements ScrapperClient {

    private static final String TG_CHAT_ID_HEADER = "Tg-Chat-Id";

    private final RestClient scrapperRestClient;
    private final RetryableHttpStatusClassifier retryableStatusClassifier;

    @Override
    @Retry(name = "scrapper")
    @CircuitBreaker(name = "scrapper")
    public void registerChat(long chatId) {
        try {
            scrapperRestClient.post().uri("/tg-chat/{id}", chatId).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw new ChatAlreadyExistsException("Chat already exists", e);
        } catch (HttpClientErrorException e) {
            throw scrapperClientStatusException("Failed to register chat in scrapper", e);
        } catch (RestClientResponseException e) {
            throw scrapperStatusException("Failed to register chat in scrapper", e);
        } catch (RestClientException e) {
            throw new ScrapperClientException("Failed to register chat in scrapper", e);
        }
    }

    @Override
    @Retry(name = "scrapper")
    @CircuitBreaker(name = "scrapper")
    public void deleteChat(long chatId) {
        try {
            scrapperRestClient
                    .method(HttpMethod.DELETE)
                    .uri("/tg-chat/{id}", chatId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ChatNotRegisteredException("Chat not found", e);
        } catch (HttpClientErrorException e) {
            throw scrapperClientStatusException("Failed to delete chat in scrapper", e);
        } catch (RestClientResponseException e) {
            throw scrapperStatusException("Failed to delete chat in scrapper", e);
        } catch (RestClientException e) {
            throw new ScrapperClientException("Failed to delete chat in scrapper", e);
        }
    }

    @Override
    @Retry(name = "scrapper")
    @CircuitBreaker(name = "scrapper")
    public ListLinksResponse getLinks(long chatId) {
        try {
            ListLinksResponse response = scrapperRestClient
                    .get()
                    .uri("/links")
                    .header(TG_CHAT_ID_HEADER, String.valueOf(chatId))
                    .retrieve()
                    .body(ListLinksResponse.class);

            return response != null
                    ? response
                    : new ListLinksResponse().links(List.of()).size(0);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ChatNotRegisteredException("Chat not found", e);
        } catch (HttpClientErrorException e) {
            throw scrapperClientStatusException("Failed to get links from scrapper", e);
        } catch (RestClientResponseException e) {
            throw scrapperStatusException("Failed to get links from scrapper", e);
        } catch (RestClientException e) {
            throw new ScrapperClientException("Failed to get links from scrapper", e);
        }
    }

    @Override
    @Retry(name = "scrapper")
    @CircuitBreaker(name = "scrapper")
    public LinkResponse addLink(long chatId, AddLinkRequest request) {
        try {
            return scrapperRestClient
                    .post()
                    .uri("/links")
                    .header(TG_CHAT_ID_HEADER, String.valueOf(chatId))
                    .body(request)
                    .retrieve()
                    .body(LinkResponse.class);
        } catch (HttpClientErrorException.Conflict e) {
            throw new DuplicateLinkException("Link already tracked", e);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ChatNotRegisteredException("Chat not found", e);
        } catch (HttpClientErrorException e) {
            throw scrapperClientStatusException("Failed to add link in scrapper", e);
        } catch (RestClientResponseException e) {
            throw scrapperStatusException("Failed to add link in scrapper", e);
        } catch (RestClientException e) {
            throw new ScrapperClientException("Failed to add link in scrapper", e);
        }
    }

    @Override
    @Retry(name = "scrapper")
    @CircuitBreaker(name = "scrapper")
    public LinkResponse removeLink(long chatId, RemoveLinkRequest request) {
        try {
            return scrapperRestClient
                    .method(HttpMethod.DELETE)
                    .uri("/links")
                    .header(TG_CHAT_ID_HEADER, String.valueOf(chatId))
                    .body(request)
                    .retrieve()
                    .body(LinkResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new TrackedLinkNotFoundException("Chat or link not found", e);
        } catch (HttpClientErrorException e) {
            throw scrapperClientStatusException("Failed to remove link in scrapper", e);
        } catch (RestClientResponseException e) {
            throw scrapperStatusException("Failed to remove link in scrapper", e);
        } catch (RestClientException e) {
            throw new ScrapperClientException("Failed to remove link in scrapper", e);
        }
    }

    private ScrapperClientException scrapperStatusException(String message, RestClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        String statusMessage = "%s, status=%d".formatted(message, statusCode);
        if (retryableStatusClassifier.isRetryable(statusCode)) {
            return new RetryableScrapperClientException(statusMessage, statusCode, e);
        }
        return new ScrapperClientException(statusMessage, e);
    }

    private RuntimeException scrapperClientStatusException(String message, HttpClientErrorException e) {
        int statusCode = e.getStatusCode().value();
        if (retryableStatusClassifier.isRetryable(statusCode)) {
            return new RetryableScrapperClientException("%s, status=%d".formatted(message, statusCode), statusCode, e);
        }
        return e;
    }
}
