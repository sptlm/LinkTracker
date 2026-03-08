package backend.academy.linktracker.bot.bot;

import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.command.CommandRegistry;
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

    @Override
    public int process(List<Update> updates) {
        for (Update update : updates) {
            processUpdate(update);
        }
        return CONFIRMED_UPDATES_ALL;
    }

    private void processUpdate(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return;
        }

        Message message = update.message();
        String messageText = message.text().trim();

        if (messageText.isBlank()) {
            return;
        }

        CommandContext context = new CommandContext(bot, message);

        log.atInfo()
                .addKeyValue("chatId", context.chatId())
                .addKeyValue("username", context.username())
                .addKeyValue("messageText", messageText)
                .log("Received message");

        if (messageText.startsWith("/")) {
            processCommand(context, messageText);
            return;
        }

        if (trackDialogService.processIfActive(context)) {
            log.atInfo().addKeyValue("chatId", context.chatId()).log("Track dialog step processed");
        }
    }

    private void processCommand(CommandContext context, String messageText) {
        String commandName = commandRegistry.extractCommandName(messageText);

        if (trackDialogService.hasActiveDialog(context.chatId()) && !"/cancel".equals(commandName)) {
            trackDialogService.cancel(context.chatId());

            log.atInfo()
                    .addKeyValue("chatId", context.chatId())
                    .addKeyValue("cancelledByCommand", commandName)
                    .log("Active dialog cancelled by another command");
        }

        commandRegistry
                .find(commandName)
                .ifPresentOrElse(
                        command -> {
                            try {
                                log.atInfo()
                                        .addKeyValue("chatId", context.chatId())
                                        .addKeyValue("command", commandName)
                                        .log("Command dispatched");

                                command.handle(context);
                            } catch (Exception e) {
                                log.atError()
                                        .setCause(e)
                                        .addKeyValue("chatId", context.chatId())
                                        .addKeyValue("command", commandName)
                                        .log("Command handling failed");

                                context.reply(messages.scrapperUnavailable());
                            }
                        },
                        () -> {
                            log.atInfo()
                                    .addKeyValue("chatId", context.chatId())
                                    .addKeyValue("command", commandName)
                                    .log("Unknown command received");

                            context.reply(messages.unknownCommand());
                        });
    }
}
