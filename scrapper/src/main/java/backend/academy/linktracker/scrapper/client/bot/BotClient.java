package backend.academy.linktracker.scrapper.client.bot;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;

public interface BotClient {

    void sendUpdate(LinkUpdate request);
}
