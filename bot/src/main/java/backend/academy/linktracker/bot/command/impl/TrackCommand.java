package backend.academy.linktracker.bot.command.impl;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.TrackDialogService;
import backend.academy.linktracker.bot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackCommand implements Command {

    private final TrackDialogService trackDialogService;
    private final UserService userService;
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
        if (!userService.isRegistered(context.userId())) {
            context.reply(messages.chatNotRegistered());
            return;
        }

        trackDialogService.start(context);
        context.reply(messages.trackStarted());
    }
}
