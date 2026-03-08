package backend.academy.linktracker.bot.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.api.dto.LinkUpdateRequest;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BotUpdateServiceTest {

    @Mock
    private TelegramBot bot;

    @Mock
    private BotMessagesService messages;

    @InjectMocks
    private BotUpdateService botUpdateService;

    @Test
    void processUpdate_sendsMessageToAllNonNullChatIds() {
        LinkUpdateRequest request = org.mockito.Mockito.mock(LinkUpdateRequest.class);

        when(request.url()).thenReturn("https://github.com/user/repo");
        when(request.description()).thenReturn("Новый коммит");
        when(request.tgChatIds()).thenReturn(List.of(101L, 202L));
        when(messages.updatesMessage("https://github.com/user/repo", "Новый коммит"))
                .thenReturn("Обновление");

        botUpdateService.processUpdate(request);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot, times(2)).execute(captor.capture());

        List<SendMessage> requests = captor.getAllValues();
        org.junit.jupiter.api.Assertions.assertEquals(2, requests.size());
    }

    @Test
    void processUpdate_whenChatIdsNull_doesNothing() {
        LinkUpdateRequest request = org.mockito.Mockito.mock(LinkUpdateRequest.class);

        when(request.url()).thenReturn("https://github.com/user/repo");
        when(request.description()).thenReturn("Новый коммит");
        when(request.tgChatIds()).thenReturn(null);
        when(messages.updatesMessage("https://github.com/user/repo", "Новый коммит"))
                .thenReturn("Обновление");

        botUpdateService.processUpdate(request);

        verify(bot, times(0)).execute(any(SendMessage.class));
    }
}
