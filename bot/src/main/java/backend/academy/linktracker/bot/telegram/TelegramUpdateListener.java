package backend.academy.linktracker.bot.telegram;

import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.command.CommandRegistry;
import backend.academy.linktracker.bot.metrics.BotMetrics;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.TrackDialogService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramUpdateListener implements UpdatesListener {

    private final TelegramBot bot;
    private final CommandRegistry commandRegistry;
    private final BotMessagesService messages;
    private final TrackDialogService trackDialogService;
    private final BotMetrics metrics;

    @Override
    public int process(List<Update> updates) {
        for (Update update : updates) {
            try {
                processUpdate(update);
            } catch (Exception e) {
                log.atError()
                        .setCause(e)
                        .addKeyValue("updateId", update.updateId())
                        .log("Failed to process update");
            }
        }
        return CONFIRMED_UPDATES_ALL;
    }

    private void processUpdate(Update update) {
        if (update.message() == null || update.message().text() == null) {
            metrics.recordTelegramRequest("unsupported");
            return;
        }

        Message message = update.message();
        if (message.from() == null) {
            log.atWarn().addKeyValue("chatId", message.chat().id()).log("Ignoring update without sender information");
            return;
        }

        String messageText = message.text().trim();
        if (messageText.isBlank()) {
            metrics.recordTelegramRequest("blank_message");
            return;
        }

        CommandContext context = new CommandContext(bot, message);

        log.atInfo()
                .addKeyValue("chatId", context.chatId())
                .addKeyValue("userId", context.userId())
                .addKeyValue("username", context.username())
                .addKeyValue("messageText", messageText)
                .log("Received message");

        if (messageText.startsWith("/")) {
            metrics.recordTelegramRequest("command");
            processCommand(context, messageText);
            return;
        }

        if (trackDialogService.processIfActive(context)) {
            metrics.recordTelegramRequest("dialog");
            log.atInfo()
                    .addKeyValue("chatId", context.chatId())
                    .addKeyValue("userId", context.userId())
                    .log("Track dialog step processed");
            return;
        }

        metrics.recordTelegramRequest("message");
    }

    private void processCommand(CommandContext context, String messageText) {
        String commandName = extractCommandName(messageText);
        var registeredCommand = commandRegistry.find(commandName);
        String safeCommandName = registeredCommand.isPresent() ? commandName : "unknown";
        metrics.recordCommandRequest(safeCommandName);

        if (trackDialogService.hasActiveDialog(context) && !"/cancel".equals(commandName)) {
            trackDialogService.cancel(context);

            log.atInfo()
                    .addKeyValue("chatId", context.chatId())
                    .addKeyValue("userId", context.userId())
                    .addKeyValue("cancelledByCommand", commandName)
                    .log("Active dialog cancelled by another command");
        }

        registeredCommand.ifPresentOrElse(
                handledCommand -> {
                    long startedAt = System.nanoTime();
                    try {
                        log.atInfo()
                                .addKeyValue("chatId", context.chatId())
                                .addKeyValue("userId", context.userId())
                                .addKeyValue("command", commandName)
                                .log("Command dispatched");

                        handledCommand.handle(context);
                    } catch (Exception e) {
                        log.atError()
                                .setCause(e)
                                .addKeyValue("chatId", context.chatId())
                                .addKeyValue("userId", context.userId())
                                .addKeyValue("command", commandName)
                                .log("Command handling failed");

                        context.reply(messages.scrapperUnavailable());
                    } finally {
                        metrics.recordCommandHandlingDuration(safeCommandName, startedAt);
                    }
                },
                () -> {
                    log.atInfo()
                            .addKeyValue("chatId", context.chatId())
                            .addKeyValue("userId", context.userId())
                            .addKeyValue("command", commandName)
                            .log("Unknown command received");

                    context.reply(messages.unknownCommand());
                });
    }

    private String extractCommandName(String text) {
        return text.trim().split("\\s+", 2)[0];
    }
}
