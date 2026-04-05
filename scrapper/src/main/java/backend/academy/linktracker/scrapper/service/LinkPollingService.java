package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.ScrapperPollingProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.updater.LinkUpdateCheckResult;
import backend.academy.linktracker.scrapper.updater.LinkUpdater;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final ScrapperPollingProperties pollingProperties;

    public void pollUpdates() {
        long offset = 0;
        int batchSize = pollingProperties.getBatchSize();
        int workerThreads = pollingProperties.getWorkerThreads();

        try (ExecutorService executor = Executors.newFixedThreadPool(workerThreads)) {
            while (true) {
                List<TrackedLink> links = linkRepository.findPage(offset, batchSize);
                if (links.isEmpty()) {
                    return;
                }

                processBatchInParallel(links, executor);
                offset += links.size();
            }
        }
    }

    private void processBatchInParallel(List<TrackedLink> links, ExecutorService executor) {
        List<CompletableFuture<Void>> tasks = new ArrayList<>(links.size());
        for (TrackedLink link : links) {
            tasks.add(CompletableFuture.runAsync(() -> pollSingleLinkSafe(link), executor));
        }
        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
    }

    private void pollSingleLinkSafe(TrackedLink link) {
        try {
            pollSingleLink(link);
        } catch (ExternalServiceException e) {
            log.atWarn()
                    .addKeyValue("linkId", link.id())
                    .addKeyValue("url", link.url())
                    .addKeyValue("reason", e.getMessage())
                    .log("Failed to poll link due to temporary external API issue");
            notifyAboutFailedProcessing(link);
        } catch (Exception e) {
            log.atError()
                    .setCause(e)
                    .addKeyValue("linkId", link.id())
                    .addKeyValue("url", link.url())
                    .log("Failed to poll link");
            notifyAboutFailedProcessing(link);
        }
    }

    private void pollSingleLink(TrackedLink link) {
        Instant checkedAt = Instant.now();

        LinkUpdater updater = findUpdater(link);
        LinkUpdateCheckResult result = updater.check(link);

        Instant updatedAt = result.changed()
                ? (result.newUpdatedAt() != null ? result.newUpdatedAt() : checkedAt)
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

        LinkUpdate request = new LinkUpdate()
                .id(link.id())
                .url(URI.create(link.url()))
                .description(result.description())
                .tgChatIds(chatIds);

        updatePublisher.publish(request);

        log.atInfo()
                .addKeyValue("linkId", link.id())
                .addKeyValue("url", link.url())
                .addKeyValue("chatIds", chatIds)
                .log("Link update detected");
    }

    private void notifyAboutFailedProcessing(TrackedLink link) {
        List<Long> chatIds = subscriptionRepository.findChatIdsByLinkId(link.id());
        if (chatIds.isEmpty()) {
            return;
        }

        LinkUpdate request = new LinkUpdate()
                .id(link.id())
                .url(URI.create(link.url()))
                .description("Не удалось обработать ссылку в текущем цикле: %s".formatted(link.url()))
                .tgChatIds(chatIds);
        updatePublisher.publish(request);
    }

    private LinkUpdater findUpdater(TrackedLink link) {
        return linkUpdaters.stream()
                .filter(updater -> updater.supports(link))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No updater for link type: " + link.type()));
    }
}
import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
