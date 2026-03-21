package backend.academy.linktracker.bot.bot;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandRegistry;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.response.BaseResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramBotServiceTest {

    @Mock
    private TelegramBot bot;

    @Mock
    private CommandRegistry commandRegistry;

    @Mock
    private TelegramUpdateListener updateListener;

    @Mock
    private Command command;

    @Mock
    private BaseResponse response;

    private TelegramBotService telegramBotService;

    @BeforeEach
    void setUp() {
        telegramBotService = new TelegramBotService(bot, commandRegistry, updateListener);
    }

    @Test
    void shouldStartPollingOnlyAfterExplicitStart() {
        verify(bot, never()).setUpdatesListener(any(), any());

        when(commandRegistry.getCommands()).thenReturn(List.of(command));
        when(command.command()).thenReturn("/start");
        when(command.description()).thenReturn("start");
        when(bot.execute(any())).thenReturn(response);
        when(response.isOk()).thenReturn(true);

        telegramBotService.start();

        verify(bot).setUpdatesListener(updateListener, any());
    }

    @Test
    void shouldStopPollingOnlyIfStarted() {
        telegramBotService.stop();
        verify(bot, never()).removeGetUpdatesListener();

        when(commandRegistry.getCommands()).thenReturn(List.of());
        when(bot.execute(any())).thenReturn(response);
        when(response.isOk()).thenReturn(true);

        telegramBotService.start();
        telegramBotService.stop();

        verify(bot).removeGetUpdatesListener();
    }
}
