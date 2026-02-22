package backend.academy.linktracker.bot.command;

public record CommandContext(long chatId, String username, String firstName, String lastName, String messageText) {}
