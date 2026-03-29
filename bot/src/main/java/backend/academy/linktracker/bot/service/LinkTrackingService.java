package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.client.scrapper.ChatAlreadyExistsException;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClient;
import backend.academy.linktracker.scrapper.generated.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.generated.dto.LinkResponse;
import backend.academy.linktracker.scrapper.generated.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.generated.dto.RemoveLinkRequest;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LinkTrackingService {

    private final ScrapperClient scrapperClient;

    public ListLinksResponse getLinks(long chatId) {
        ensureChatRegistered(chatId);
        return scrapperClient.getLinks(chatId);
    }

    public LinkResponse addLink(long chatId, String link, List<String> tags) {
        ensureChatRegistered(chatId);
        return scrapperClient.addLink(
                chatId, new AddLinkRequest().link(URI.create(link)).tags(tags).filters(List.of()));
    }

    public LinkResponse removeLink(long chatId, String link) {
        ensureChatRegistered(chatId);
        return scrapperClient.removeLink(chatId, new RemoveLinkRequest().link(URI.create(link)));
    }

    private void ensureChatRegistered(long chatId) {
        try {
            scrapperClient.registerChat(chatId);
        } catch (ChatAlreadyExistsException ignored) {
            log.debug("Chat {} is already registered in scrapper", chatId);
        }
    }
}
