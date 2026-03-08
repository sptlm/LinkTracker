package backend.academy.linktracker.bot.command.impl;

import backend.academy.linktracker.bot.client.scrapper.ChatAlreadyExistsException;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClientException;
import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.LinkTrackingService;
import backend.academy.linktracker.bot.service.RegistrationResult;
import backend.academy.linktracker.bot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartCommand implements Command {

    private final UserService userService;
    private final LinkTrackingService linkTrackingService;
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

        if (result.created()) {
            try {
                linkTrackingService.registerChat(context.chatId());
            } catch (ScrapperClientException e) {
            log.atWarn()
                .setCause(e)
                .addKeyValue("chatId", context.chatId())
                .log("Failed to register chat in scrapper");
        }

            context.reply(messages.startWelcome(result.user().displayName()));
            return;
        }

        context.reply(messages.welcomeBack(result.user().displayName()));
    }
}
