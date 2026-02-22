package backend.academy.linktracker.bot.bot;

import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.command.CommandDispatcher;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramUpdateListener implements UpdatesListener {

    private final TelegramBot bot;
    private final CommandDispatcher dispatcher;

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
        long chatId = message.chat().id();
        String messageText = message.text().trim();

        // Строим контекст из данных Telegram-сообщения
        // message.from() — отправитель (null в каналах, но не в личных чатах)
        var from = message.from();
        CommandContext context = new CommandContext(
                chatId,
                from != null ? from.username() : null,
                from != null ? from.firstName() : null,
                from != null ? from.lastName() : null,
                messageText);

        log.atInfo()
                .addKeyValue("chatId", chatId)
                .addKeyValue("username", context.username())
                .addKeyValue("messageText", messageText)
                .log("Received message");

        if (!messageText.startsWith("/")) {
            return;
        }

        String response = dispatcher.dispatch(context);

        log.atInfo()
                .addKeyValue("chatId", chatId)
                .addKeyValue("command", messageText.split("\\s+")[0])
                .log("Command dispatched");

        bot.execute(new SendMessage(chatId, response));
    }
}
