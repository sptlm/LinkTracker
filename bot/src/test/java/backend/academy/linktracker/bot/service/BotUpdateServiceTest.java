package backend.academy.linktracker.bot.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.bot.metrics.BotMetrics;
import backend.academy.linktracker.bot.service.exception.UpdateDeliveryException;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BotUpdateServiceTest {

    @Mock
    private TelegramBot bot;

    @Mock
    private BotMessagesService messages;

    @Mock
    private BotMetrics metrics;

    @Mock
    private SendResponse sendResponse;

    private BotUpdateService service;

    @BeforeEach
    void setUp() {
        service = new BotUpdateService(bot, messages, metrics);
    }

    @Test
    void shouldRecordSentNotificationOnlyWhenTelegramAcceptsMessage() {
        LinkUpdate update = update();
        when(messages.updatesMessage(String.valueOf(update.getUrl()), update.getDescription()))
                .thenReturn("message text");
        when(bot.execute(any(SendMessage.class))).thenReturn(sendResponse);
        when(sendResponse.isOk()).thenReturn(true);

        service.processUpdateForChat(update, 100L);

        verify(metrics).recordSentNotification();
    }

    @Test
    void shouldNotRecordSentNotificationWhenTelegramRejectsMessage() {
        LinkUpdate update = update();
        when(messages.updatesMessage(String.valueOf(update.getUrl()), update.getDescription()))
                .thenReturn("message text");
        when(bot.execute(any(SendMessage.class))).thenReturn(sendResponse);
        when(sendResponse.isOk()).thenReturn(false);
        when(sendResponse.description()).thenReturn("chat not found");

        assertThrows(UpdateDeliveryException.class, () -> service.processUpdateForChat(update, 100L));

        verify(metrics, never()).recordSentNotification();
    }

    private LinkUpdate update() {
        return new LinkUpdate()
                .id(1L)
                .url(URI.create("https://github.com/user/repo"))
                .description("Repository updated")
                .tgChatIds(List.of(100L));
    }
}
