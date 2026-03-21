package backend.academy.linktracker.bot.client.scrapper;

import backend.academy.linktracker.scrapper.generated.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.generated.dto.LinkResponse;
import backend.academy.linktracker.scrapper.generated.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.generated.dto.RemoveLinkRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class HttpScrapperClient implements ScrapperClient {

    private static final String TG_CHAT_ID_HEADER = "Tg-Chat-Id";

    private final RestClient scrapperRestClient;

    @Override
    public void registerChat(long chatId) {
        try {
            scrapperRestClient.post().uri("/tg-chat/{id}", chatId).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw new ChatAlreadyExistsException("Chat already exists", e);
        } catch (RestClientException e) {
            throw new ScrapperClientException("Failed to register chat in scrapper", e);
        }
    }

    @Override
    public void deleteChat(long chatId) {
        try {
            scrapperRestClient
                    .method(HttpMethod.DELETE)
                    .uri("/tg-chat/{id}", chatId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ChatNotRegisteredException("Chat not found", e);
        } catch (RestClientException e) {
            throw new ScrapperClientException("Failed to delete chat in scrapper", e);
        }
    }

    @Override
    public ListLinksResponse getLinks(long chatId) {
        try {
            ListLinksResponse response = scrapperRestClient
                    .get()
                    .uri("/links")
                    .header(TG_CHAT_ID_HEADER, String.valueOf(chatId))
                    .retrieve()
                    .body(ListLinksResponse.class);

            return response != null ? response : new ListLinksResponse().links(List.of()).size(0);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ChatNotRegisteredException("Chat not found", e);
        } catch (RestClientException e) {
            throw new ScrapperClientException("Failed to get links from scrapper", e);
        }
    }

    @Override
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
        } catch (RestClientException e) {
            throw new ScrapperClientException("Failed to add link in scrapper", e);
        }
    }

    @Override
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
        } catch (RestClientException e) {
            throw new ScrapperClientException("Failed to remove link in scrapper", e);
        }
    }
}
