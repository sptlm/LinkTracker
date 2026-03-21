package backend.academy.linktracker.bot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.command.impl.TrackCommand;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.TrackDialogService;
import backend.academy.linktracker.bot.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackCommandTest {

    @Mock
    private TrackDialogService trackDialogService;

    @Mock
    private UserService userService;

    @Mock
    private BotMessagesService messages;

    @Mock
    private CommandContext context;

    @InjectMocks
    private TrackCommand trackCommand;

    @Test
    void command_returnsTrackCommand() {
        assertEquals("/track", trackCommand.command());
    }

    @Test
    void description_returnsExpectedDescription() {
        assertEquals("Начать отслеживание ссылки", trackCommand.description());
    }

    @Test
    void handle_whenUserIsNotRegistered_repliesChatNotRegistered() {
        when(context.userId()).thenReturn(123L);
        when(userService.isRegistered(123L)).thenReturn(false);
        when(messages.chatNotRegistered()).thenReturn("Сначала зарегистрируйтесь через /start");

        trackCommand.handle(context);

        verify(context).reply("Сначала зарегистрируйтесь через /start");
        verify(trackDialogService, never()).start(context);
    }

    @Test
    void handle_whenUserIsRegistered_startsDialogAndRepliesTrackStarted() {
        when(context.userId()).thenReturn(123L);
        when(userService.isRegistered(123L)).thenReturn(true);
        when(messages.trackStarted()).thenReturn("Отправьте ссылку");

        trackCommand.handle(context);

        verify(trackDialogService).start(context);
        verify(context).reply("Отправьте ссылку");
    }
}
