package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.api.dto.LinkUpdateRequest;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotUpdateService {

    private final TelegramBot bot;
    private final BotMessagesService messages;

    public void processUpdate(LinkUpdateRequest request) {
        String text = messages.updatesMessage(request.url(), request.description());

        for (Long chatId : request.tgChatIds()) {
            log.atInfo()
                    .addKeyValue("linkId", request.id())
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("url", request.url())
                    .log("Sending link update notification");

            bot.execute(new SendMessage(chatId, text));
        }
    }
}
