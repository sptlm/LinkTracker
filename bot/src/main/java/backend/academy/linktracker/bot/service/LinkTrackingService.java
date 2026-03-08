package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.client.scrapper.ScrapperClient;
import backend.academy.linktracker.bot.client.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.bot.client.scrapper.dto.LinkResponse;
import backend.academy.linktracker.bot.client.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.bot.client.scrapper.dto.RemoveLinkRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkTrackingService {

    private final ScrapperClient scrapperClient;

    public void registerChat(long chatId) {
        scrapperClient.registerChat(chatId);
    }

    public void deleteChat(long chatId) {
        scrapperClient.deleteChat(chatId);
    }

    public ListLinksResponse getLinks(long chatId) {
        return scrapperClient.getLinks(chatId);
    }

    public LinkResponse addLink(long chatId, String link, List<String> tags, List<String> filters) {
        return scrapperClient.addLink(chatId, new AddLinkRequest(link, tags, filters));
    }

    public LinkResponse removeLink(long chatId, String link) {
        return scrapperClient.removeLink(chatId, new RemoveLinkRequest(link));
    }
}
