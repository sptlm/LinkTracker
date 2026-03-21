package backend.academy.linktracker.bot.command.impl;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.BotRegistrationService;
import backend.academy.linktracker.bot.service.RegistrationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommand implements Command {

    private final BotRegistrationService botRegistrationService;
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
        RegistrationResult result = botRegistrationService.ensureRegistered(context);
        if (result.created()) {
            context.reply(messages.startWelcome(result.user().displayName()));
            return;
        }

        context.reply(messages.welcomeBack(result.user().displayName()));
    }
}
