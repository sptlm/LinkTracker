package backend.academy.linktracker.bot.bot;

import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.command.CommandRegistry;
import backend.academy.linktracker.bot.service.BotMessagesService;
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

        if (messageText.isBlank() || !messageText.startsWith("/")) {
            return;
        }

        CommandContext context = new CommandContext(bot, message);
        String commandName = commandRegistry.extractCommandName(messageText);

        log.atInfo()
                .addKeyValue("chatId", context.chatId())
                .addKeyValue("username", context.username())
                .addKeyValue("messageText", messageText)
                .log("Received command message");

        commandRegistry
                .find(commandName)
                .ifPresentOrElse(
                        command -> {
                            log.atInfo()
                                    .addKeyValue("chatId", context.chatId())
                                    .addKeyValue("command", commandName)
                                    .log("Command dispatched");

                            command.handle(context);
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
