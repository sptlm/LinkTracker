package backend.academy.linktracker.bot.command.impl;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.command.CommandDispatcher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand implements Command {

    private final CommandDispatcher dispatcher;

    public HelpCommand(@Lazy CommandDispatcher dispatcher) {
        this.dispatcher = dispatcher;
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
    public String handle(CommandContext context) {
        StringBuilder sb = new StringBuilder("Доступные команды:\n");
        for (Command cmd : dispatcher.getCommands()) {
            sb.append(cmd.command()).append(" — ").append(cmd.description()).append("\n");
        }
        return sb.toString().trim();
    }
}
