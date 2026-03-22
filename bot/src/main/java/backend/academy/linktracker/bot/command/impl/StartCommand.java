package backend.academy.linktracker.bot.command.impl;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.service.BotMessagesService;
import org.springframework.stereotype.Component;

@Component
public class StartCommand implements Command {

    private final BotMessagesService messages;

    public StartCommand(BotMessagesService messages) {
        this.messages = messages;
    }

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public String description() {
        return "Начало работы с ботом";
    }

    @Override
    public void handle(CommandContext context) {
        context.reply(messages.startWelcome(resolveDisplayName(context)));
    }

    private String resolveDisplayName(CommandContext context) {
        if (context.firstName() != null && !context.firstName().isBlank()) {
            return context.firstName();
        }
        if (context.username() != null && !context.username().isBlank()) {
            return "@" + context.username();
        }
        return "пользователь";
    }
}
