package backend.academy.linktracker.bot.command.impl;

import backend.academy.linktracker.bot.client.scrapper.ChatNotRegisteredException;
import backend.academy.linktracker.bot.client.scrapper.TrackedLinkNotFoundException;
import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.LinkTrackingService;
import backend.academy.linktracker.bot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UntrackCommand implements Command {

    private final LinkTrackingService linkTrackingService;
    private final UserService userService;
    private final BotMessagesService messages;

    @Override
    public String command() {
        return "/untrack";
    }

    @Override
    public String description() {
        return "Прекратить отслеживание ссылки";
    }

    @Override
    public void handle(CommandContext context) {
        if (!userService.isRegistered(context.userId())) {
            context.reply(messages.chatNotRegistered());
            return;
        }

        String link = extractLink(context.messageText());
        if (link == null) {
            context.reply(messages.untrackUsage());
            return;
        }

        try {
            linkTrackingService.removeLink(context.chatId(), link);
            context.reply(messages.linkRemoved());
        } catch (TrackedLinkNotFoundException e) {
            context.reply(messages.linkNotFound());
        } catch (ChatNotRegisteredException e) {
            context.reply(messages.chatNotRegistered());
        }
    }

    private String extractLink(String messageText) {
        String[] parts = messageText.trim().split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            return null;
        }
        return parts[1].trim();
    }
}
