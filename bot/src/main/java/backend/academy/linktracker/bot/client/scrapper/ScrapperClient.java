package backend.academy.linktracker.bot.client.scrapper;

import backend.academy.linktracker.scrapper.generated.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.generated.dto.LinkResponse;
import backend.academy.linktracker.scrapper.generated.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.generated.dto.RemoveLinkRequest;

public interface ScrapperClient {

    void registerChat(long chatId);

    void deleteChat(long chatId);

    ListLinksResponse getLinks(long chatId);

    LinkResponse addLink(long chatId, AddLinkRequest request);

    LinkResponse removeLink(long chatId, RemoveLinkRequest request);
}
