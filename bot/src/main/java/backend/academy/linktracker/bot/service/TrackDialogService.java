package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.client.scrapper.ChatNotRegisteredException;
import backend.academy.linktracker.bot.client.scrapper.DuplicateLinkException;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClientException;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.model.DialogSessionKey;
import backend.academy.linktracker.bot.repository.UserDialogStateRepository;
import backend.academy.linktracker.bot.state.TrackDialogState;
import backend.academy.linktracker.bot.state.TrackDialogStep;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TrackDialogService {

    private final UserDialogStateRepository stateRepository;
    private final SupportedLinkParser supportedLinkParser;
    private final LinkTrackingService linkTrackingService;
    private final BotMessagesService messages;
    private final Map<TrackDialogStep, BiConsumer<CommandContext, TrackDialogState>> handlers;

    public TrackDialogService(
            UserDialogStateRepository stateRepository,
            SupportedLinkParser supportedLinkParser,
            LinkTrackingService linkTrackingService,
            BotMessagesService messages) {
        this.stateRepository = stateRepository;
        this.supportedLinkParser = supportedLinkParser;
        this.linkTrackingService = linkTrackingService;
        this.messages = messages;
        this.handlers = Map.of(
                TrackDialogStep.WAITING_LINK, this::handleWaitingLink,
                TrackDialogStep.WAITING_TAGS, this::handleWaitingTags,
                TrackDialogStep.WAITING_FILTERS, this::handleWaitingFilters);
    }

    public void start(CommandContext context) {
        stateRepository.save(sessionKey(context), TrackDialogState.waitingLink());
    }

    public boolean hasActiveDialog(CommandContext context) {
        return stateRepository.findById(sessionKey(context)).isPresent();
    }

    public void cancel(CommandContext context) {
        stateRepository.delete(sessionKey(context));
    }

    public boolean processIfActive(CommandContext context) {
        TrackDialogState state = stateRepository.findById(sessionKey(context)).orElse(null);
        if (state == null) {
            return false;
        }

        handlers.getOrDefault(state.step(), this::handleUnexpectedStep).accept(context, state);
        return true;
    }

    private void handleWaitingLink(CommandContext context, TrackDialogState ignored) {
        URI uri = supportedLinkParser.parse(context.messageText()).orElse(null);
        if (uri == null) {
            context.reply(messages.invalidLink());
            return;
        }

        stateRepository.save(sessionKey(context), TrackDialogState.waitingTags(uri));
        context.reply(messages.trackAskTags());
    }

    private void handleWaitingTags(CommandContext context, TrackDialogState state) {
        List<String> tags = parseCommaSeparated(context.messageText());
        stateRepository.save(sessionKey(context), TrackDialogState.waitingFilters(state.url(), tags));
        context.reply(messages.trackAskFilters());
    }

    private void handleWaitingFilters(CommandContext context, TrackDialogState state) {
        List<String> filters = parseCommaSeparated(context.messageText());

        try {
            linkTrackingService.addLink(context.chatId(), state.url().toString(), state.tags(), filters);
            stateRepository.delete(sessionKey(context));
            context.reply(messages.linkTracked());
        } catch (DuplicateLinkException e) {
            stateRepository.delete(sessionKey(context));
            context.reply(messages.alreadyTracked());
        } catch (ChatNotRegisteredException e) {
            stateRepository.delete(sessionKey(context));
            context.reply(messages.chatNotRegistered());
        } catch (ScrapperClientException e) {
            log.atWarn()
                    .setCause(e)
                    .addKeyValue("chatId", context.chatId())
                    .addKeyValue("userId", context.userId())
                    .addKeyValue("url", state.url())
                    .log("Failed to add link in scrapper");

            stateRepository.delete(sessionKey(context));
            context.reply(messages.scrapperUnavailable());
        }
    }

    private void handleUnexpectedStep(CommandContext context, TrackDialogState state) {
        log.atWarn()
                .addKeyValue("chatId", context.chatId())
                .addKeyValue("userId", context.userId())
                .addKeyValue("step", state.step())
                .log("Unknown dialog state encountered");
        stateRepository.delete(sessionKey(context));
        context.reply(messages.nothingToCancel());
    }

    private DialogSessionKey sessionKey(CommandContext context) {
        return new DialogSessionKey(context.chatId(), context.userId());
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
