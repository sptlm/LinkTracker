package backend.academy.linktracker.scrapper.metrics;

import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TrackedLinksMetricsInitializer {

    private final LinkRepository linkRepository;
    private final ScrapperMetrics metrics;

    public TrackedLinksMetricsInitializer(LinkRepository linkRepository, ScrapperMetrics metrics) {
        this.linkRepository = linkRepository;
        this.metrics = metrics;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        for (LinkSourceType type : LinkSourceType.values()) {
            metrics.setTrackedLinks(type, linkRepository.countByType(type));
        }
    }
}
