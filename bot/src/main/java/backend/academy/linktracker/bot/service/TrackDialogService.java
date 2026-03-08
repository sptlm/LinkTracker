package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.client.scrapper.ChatNotRegisteredException;
import backend.academy.linktracker.bot.client.scrapper.DuplicateLinkException;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClientException;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.repository.UserDialogStateRepository;
import backend.academy.linktracker.bot.state.TrackDialogState;
import backend.academy.linktracker.bot.state.TrackDialogStep;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TrackDialogService {

    private final UserDialogStateRepository stateRepository;
    private final SupportedLinkParser supportedLinkParser;
    private final LinkTrackingService linkTrackingService;
    private final BotMessagesService messages;

    public TrackDialogService(
            UserDialogStateRepository stateRepository,
            SupportedLinkParser supportedLinkParser,
            LinkTrackingService linkTrackingService,
            BotMessagesService messages) {
        this.stateRepository = stateRepository;
        this.supportedLinkParser = supportedLinkParser;
        this.linkTrackingService = linkTrackingService;
        this.messages = messages;
    }

    public void start(long chatId) {
        stateRepository.save(chatId, TrackDialogState.waitingLink());
    }

    public boolean hasActiveDialog(long chatId) {
        return stateRepository.findByChatId(chatId).isPresent();
    }

    public void cancel(long chatId) {
        stateRepository.delete(chatId);
    }

    public boolean processIfActive(CommandContext context) {
        TrackDialogState state = stateRepository.findByChatId(context.chatId()).orElse(null);
        if (state == null) {
            return false;
        }

        if (state.step() == TrackDialogStep.WAITING_LINK) {
            handleWaitingLink(context);
            return true;
        }

        if (state.step() == TrackDialogStep.WAITING_TAGS) {
            handleWaitingTags(context, state);
            return true;
        }

        handleWaitingFilters(context, state);
        return true;
    }

    private void handleWaitingLink(CommandContext context) {
        URI uri = supportedLinkParser.parse(context.messageText()).orElse(null);
        if (uri == null) {
            context.reply(messages.invalidLink());
            return;
        }

        stateRepository.save(context.chatId(), TrackDialogState.waitingTags(uri));
        context.reply(messages.trackAskTags());
    }

    private void handleWaitingTags(CommandContext context, TrackDialogState state) {
        List<String> tags = parseCommaSeparated(context.messageText());
        stateRepository.save(context.chatId(), TrackDialogState.waitingFilters(state.url(), tags));
        context.reply(messages.trackAskFilters());
    }

    private void handleWaitingFilters(CommandContext context, TrackDialogState state) {
        List<String> filters = parseCommaSeparated(context.messageText());

        try {
            linkTrackingService.addLink(context.chatId(), state.url().toString(), state.tags(), filters);
            stateRepository.delete(context.chatId());
            context.reply(messages.linkTracked());
        } catch (DuplicateLinkException e) {
            stateRepository.delete(context.chatId());
            context.reply(messages.alreadyTracked());
        } catch (ChatNotRegisteredException e) {
            stateRepository.delete(context.chatId());
            context.reply(messages.chatNotRegistered());
        } catch (ScrapperClientException e) {
            log.atWarn()
                    .setCause(e)
                    .addKeyValue("chatId", context.chatId())
                    .addKeyValue("url", state.url())
                    .log("Failed to add link in scrapper");

            stateRepository.delete(context.chatId());
            context.reply(messages.scrapperUnavailable());
        }
    }

    private List<String> parseCommaSeparated(String text) {
        if (text == null) {
            return List.of();
        }

        String normalized = text.trim();
        if (normalized.isBlank() || "-".equals(normalized)) {
            return List.of();
        }

        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(val -> !val.isBlank())
                .distinct()
                .toList();
    }
}
