package backend.academy.linktracker.scrapper.client.bot;

import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdateRequest;

public interface BotClient {

    void sendUpdate(LinkUpdateRequest request);
}
