package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.properties.ScrapperPollingProperties;
import backend.academy.linktracker.scrapper.service.LinkPollingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkPollingScheduler {

    private final LinkPollingService linkPollingService;
    private final ScrapperPollingProperties properties;

    @Scheduled(fixedDelayString = "${app.polling.interval}")
    public void poll() {
        log.atDebug()
                .addKeyValue("interval", properties.getInterval())
                .log("Polling cycle started");

        linkPollingService.pollUpdates();
    }
}
