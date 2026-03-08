package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.api.dto.LinkUpdateRequest;
import backend.academy.linktracker.bot.service.BotMessagesService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
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
        List<Long> chatIds = request.tgChatIds() == null ? List.of() : request.tgChatIds();

        for (Long chatId : chatIds) {
            if (chatId == null) {
                continue;
            }

            try {
                bot.execute(new SendMessage(chatId, text));

                log.atInfo()
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("linkId", request.id())
                    .addKeyValue("url", request.url())
                    .log("Update notification sent");
            } catch (Exception e) {
                log.atWarn()
                    .setCause(e)
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("linkId", request.id())
                    .addKeyValue("url", request.url())
                    .log("Failed to send update notification");
            }
        }
    }
}
