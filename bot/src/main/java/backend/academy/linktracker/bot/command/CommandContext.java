package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Objects;

public record CommandContext(TelegramBot bot, Message message) {

    public long chatId() {
        return message.chat().id();
    }

    public long userId() {
        return requireFrom().id();
    }

    public String messageText() {
        return message.text() == null ? "" : message.text().trim();
    }

    public User from() {
        return message.from();
    }

    public User requireFrom() {
        return Objects.requireNonNull(from(), "Telegram update does not contain sender information");
    }

    public String username() {
        return requireFrom().username();
    }

    public String firstName() {
        return requireFrom().firstName();
    }

    public String lastName() {
        return requireFrom().lastName();
    }

    public void reply(String text) {
        bot.execute(new SendMessage(chatId(), text));
    }
}
