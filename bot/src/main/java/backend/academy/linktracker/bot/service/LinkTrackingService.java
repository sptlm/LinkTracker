package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.client.scrapper.ScrapperClient;
import backend.academy.linktracker.scrapper.generated.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.generated.dto.LinkResponse;
import backend.academy.linktracker.scrapper.generated.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.generated.dto.RemoveLinkRequest;
import java.net.URI;
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

    public ListLinksResponse getLinks(long chatId) {
        return scrapperClient.getLinks(chatId);
    }

    public LinkResponse addLink(long chatId, String link, List<String> tags, List<String> filters) {
        return scrapperClient.addLink(chatId, new AddLinkRequest().link(URI.create(link)).tags(tags).filters(filters));
    }

    public LinkResponse removeLink(long chatId, String link) {
        return scrapperClient.removeLink(chatId, new RemoveLinkRequest().link(URI.create(link)));
    }
}
