package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;

public interface UpdatePublisher {

    void publish(LinkUpdate request);
}
