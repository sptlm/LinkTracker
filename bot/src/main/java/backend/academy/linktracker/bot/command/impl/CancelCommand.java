package backend.academy.linktracker.bot.command.impl;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.TrackDialogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CancelCommand implements Command {

    private final TrackDialogService trackDialogService;
    private final BotMessagesService messages;

    @Override
    public String command() {
        return "/cancel";
    }

    @Override
    public String description() {
        return "Отменить текущий диалог";
    }

    @Override
    public void handle(CommandContext context) {
        if (trackDialogService.hasActiveDialog(context)) {
            trackDialogService.cancel(context);
            context.reply(messages.trackCancelled());
            return;
        }

        context.reply(messages.nothingToCancel());
    }
}
