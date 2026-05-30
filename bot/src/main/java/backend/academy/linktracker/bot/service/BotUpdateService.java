package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.bot.metrics.BotMetrics;
import backend.academy.linktracker.bot.service.exception.UpdateDeliveryException;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
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
    private final BotMetrics metrics;

    public void processUpdate(LinkUpdate request) {
        List<Long> chatIds = request.getTgChatIds() == null ? List.of() : request.getTgChatIds();

        for (Long chatId : chatIds) {
            try {
                processUpdateForChat(request, chatId);
            } catch (UpdateDeliveryException ignored) {
                // HTTP notification processing remains best-effort for all chats.
            }
        }
    }

    public void processUpdateForChat(LinkUpdate request, Long chatId) {
        if (chatId == null) {
            return;
        }

        String text = messages.updatesMessage(String.valueOf(request.getUrl()), request.getDescription());
        try {
            SendResponse response = bot.execute(new SendMessage(chatId, text));
            if (!response.isOk()) {
                throw new UpdateDeliveryException("Telegram API rejected message: " + response.description());
            }
            metrics.recordSentNotification();

            log.atInfo()
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("linkId", request.getId())
                    .addKeyValue("url", request.getUrl())
                    .log("Update notification sent");
        } catch (UpdateDeliveryException e) {
            log.atWarn()
                    .setCause(e)
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("linkId", request.getId())
                    .addKeyValue("url", request.getUrl())
                    .log("Failed to send update notification");
            throw e;
        } catch (Exception e) {
            log.atWarn()
                    .setCause(e)
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("linkId", request.getId())
                    .addKeyValue("url", request.getUrl())
                    .log("Failed to send update notification");
            throw new UpdateDeliveryException("Failed to send update notification to chat " + chatId, e);
        }
    }
}
