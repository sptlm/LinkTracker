package backend.academy.linktracker.bot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.command.impl.CancelCommand;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.TrackDialogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelCommandTest {

    @Mock
    private TrackDialogService trackDialogService;

    @Mock
    private BotMessagesService messages;

    @Mock
    private CommandContext context;

    @InjectMocks
    private CancelCommand cancelCommand;

    @Test
    void command_returnsCancel() {
        assertEquals("/cancel", cancelCommand.command());
    }

    @Test
    void description_returnsExpectedDescription() {
        assertEquals("Отменить текущий диалог", cancelCommand.description());
    }

    @Test
    void handle_whenActiveDialogExists_cancelsAndRepliesTrackCancelled() {
        when(context.chatId()).thenReturn(123L);
        when(trackDialogService.hasActiveDialog(123L)).thenReturn(true);
        when(messages.trackCancelled()).thenReturn("Диалог отменен");

        cancelCommand.handle(context);

        verify(trackDialogService).cancel(123L);
        verify(context).reply("Диалог отменен");
    }

    @Test
    void handle_whenNoActiveDialog_repliesNothingToCancel() {
        when(context.chatId()).thenReturn(123L);
        when(trackDialogService.hasActiveDialog(123L)).thenReturn(false);
        when(messages.nothingToCancel()).thenReturn("Нечего отменять");

        cancelCommand.handle(context);

        verify(context).reply("Нечего отменять");
    }
}
