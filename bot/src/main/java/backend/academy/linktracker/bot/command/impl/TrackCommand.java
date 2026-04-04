package backend.academy.linktracker.bot.command.impl;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.TrackDialogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackCommand implements Command {

    private final TrackDialogService trackDialogService;
    private final BotMessagesService messages;

    @Override
    public String command() {
        return "/track";
    }

    @Override
    public String description() {
        return "Начать отслеживание ссылки";
    }

    @Override
    public void handle(CommandContext context) {
        trackDialogService.start(context);
        context.reply(messages.trackStarted());
    }
}
