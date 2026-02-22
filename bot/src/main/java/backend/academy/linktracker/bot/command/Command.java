package backend.academy.linktracker.bot.command;

public interface Command {

    String command();

    String description();

    String handle(CommandContext context);
}
