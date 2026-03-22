package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.properties.BotMessagesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotMessagesService {

    private final BotMessagesProperties properties;

    public String unknownCommand() {
        return properties.getUnknownCommand();
    }

    public String startWelcome(String displayName) {
        return properties.getStartWelcome().formatted(displayName);
    }

    public String welcomeBack(String displayName) {
        return properties.getWelcomeBack().formatted(displayName);
    }

    public String helpHeader() {
        return properties.getHelpHeader();
    }

    public String trackStarted() {
        return properties.getTrackStarted();
    }

    public String trackAskTags() {
        return properties.getTrackAskTags();
    }


    public String trackCancelled() {
        return properties.getTrackCancelled();
    }

    public String nothingToCancel() {
        return properties.getNothingToCancel();
    }

    public String invalidLink() {
        return properties.getInvalidLink();
    }

    public String alreadyTracked() {
        return properties.getAlreadyTracked();
    }

    public String linkTracked() {
        return properties.getLinkTracked();
    }

    public String linkRemoved() {
        return properties.getLinkRemoved();
    }

    public String linkNotFound() {
        return properties.getLinkNotFound();
    }

    public String noTrackedLinks() {
        return properties.getNoTrackedLinks();
    }

    public String chatNotRegistered() {
        return properties.getChatNotRegistered();
    }

    public String untrackUsage() {
        return properties.getUntrackUsage();
    }

    public String updatesMessage(String url, String description) {
        String safeDescription = description == null || description.isBlank() ? "Есть новые изменения." : description;
        return properties.getUpdatesTemplate().formatted(url, safeDescription);
    }

    public String scrapperUnavailable() {
        return properties.getScrapperUnavailable();
    }
}
