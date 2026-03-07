package backend.academy.linktracker.bot.command.impl;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.command.CommandRegistry;
import backend.academy.linktracker.bot.service.BotMessagesService;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand implements Command {

    @Lazy
    private final CommandRegistry commandRegistry;

    private final BotMessagesService messages;

    public HelpCommand(@Lazy CommandRegistry commandRegistry, BotMessagesService messages) {
        this.commandRegistry = commandRegistry;
        this.messages = messages;
    }

    @Override
    public String command() {
        return "/help";
    }

    @Override
    public String description() {
        return "Список доступных команд";
    }

    @Override
    public void handle(CommandContext context) {
        String commandsText = commandRegistry.getCommands().stream()
                .map(cmd -> cmd.command() + " — " + cmd.description())
                .collect(Collectors.joining("\n"));

        context.reply(messages.helpHeader() + "\n" + commandsText);
    }
}
