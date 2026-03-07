package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.SendMessage;

public record CommandContext(TelegramBot bot, Message message) {

    public long chatId() {
        return message.chat().id();
    }

    public String messageText() {
        return message.text() == null ? "" : message.text().trim();
    }

    public User from() {
        return message.from();
    }

    public String username() {
        return from() != null ? from().username() : null;
    }

    public String firstName() {
        return from() != null ? from().firstName() : null;
    }

    public String lastName() {
        return from() != null ? from().lastName() : null;
    }

    public void reply(String text) {
        bot.execute(new SendMessage(chatId(), text));
    }
}
