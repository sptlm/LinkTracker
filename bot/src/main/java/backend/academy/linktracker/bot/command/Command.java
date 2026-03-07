package backend.academy.linktracker.bot.command;

public interface Command {

    String command();

    String description();

    void handle(CommandContext context);
}
