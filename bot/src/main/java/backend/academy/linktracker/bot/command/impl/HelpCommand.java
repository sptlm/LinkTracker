package backend.academy.linktracker.bot.command.impl;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.command.CommandRegistry;
import backend.academy.linktracker.bot.service.BotMessagesService;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HelpCommand implements Command {

    private final CommandRegistry commandRegistry;
    private final BotMessagesService messages;

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
