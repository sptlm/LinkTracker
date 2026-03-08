package backend.academy.linktracker.scrapper.service;


import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdateRequest;

public interface UpdatePublisher {

    void publish(LinkUpdateRequest request);
}
