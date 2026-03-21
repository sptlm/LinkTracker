package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.updater.LinkUpdateCheckResult;
import backend.academy.linktracker.scrapper.updater.LinkUpdater;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkPollingService {

    private final LinkRepository linkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final List<LinkUpdater> linkUpdaters;
    private final UpdatePublisher updatePublisher;

    public void pollUpdates() {
        List<TrackedLink> links = linkRepository.findAll();

        for (TrackedLink link : links) {
            pollSingleLink(link);
        }
    }

    private void pollSingleLink(TrackedLink link) {
        Instant checkedAt = Instant.now();

        LinkUpdater updater = findUpdater(link);
        LinkUpdateCheckResult result = updater.check(link);

        Instant updatedAt = result.changed()
                ? (result.newUpdatedAt() != null
                        ? result.newUpdatedAt()
                        : checkedAt) // если времени обновления на сайте нет, то ставим время проверки
                : link.lastUpdatedAt();

        linkRepository.updatePollingState(link.id(), checkedAt, updatedAt);

        if (!result.changed()) {
            log.atDebug()
                    .addKeyValue("linkId", link.id())
                    .addKeyValue("url", link.url())
                    .log("No updates detected");
            return;
        }

        List<Long> chatIds = subscriptionRepository.findChatIdsByLinkId(link.id());
        if (chatIds.isEmpty()) {
            return;
        }

        LinkUpdate request = new LinkUpdate().id(link.id()).url(URI.create(link.url())).description(result.description()).tgChatIds(chatIds);

        updatePublisher.publish(request);

        log.atInfo()
                .addKeyValue("linkId", link.id())
                .addKeyValue("url", link.url())
                .addKeyValue("chatIds", chatIds)
                .log("Link update detected");
    }

    private LinkUpdater findUpdater(TrackedLink link) {
        return linkUpdaters.stream()
                .filter(updater -> updater.supports(link))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No updater for link type: " + link.type()));
    }
}
