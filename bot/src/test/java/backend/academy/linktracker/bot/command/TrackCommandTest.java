package backend.academy.linktracker.bot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.command.impl.TrackCommand;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.TrackDialogService;
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
    void handle_startsDialogAndRepliesTrackStarted() {
        when(messages.trackStarted()).thenReturn("Отправьте ссылку");

        trackCommand.handle(context);

        verify(trackDialogService).start(context);
        verify(context).reply("Отправьте ссылку");
    }
}
