package backend.academy.linktracker.bot.command.impl;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.RegistrationResult;
import backend.academy.linktracker.bot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommand implements Command {

    private final UserService userService;
    private final BotMessagesService messages;

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
        RegistrationResult result = userService.registerIfAbsent(context.message());

        String response = result.created()
                ? messages.startWelcome(result.user().displayName())
                : messages.welcomeBack(result.user().displayName());

        context.reply(response);
    }
}
