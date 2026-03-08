package backend.academy.linktracker.bot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.scrapper.ChatNotRegisteredException;
import backend.academy.linktracker.bot.client.scrapper.TrackedLinkNotFoundException;
import backend.academy.linktracker.bot.command.impl.UntrackCommand;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.LinkTrackingService;
import backend.academy.linktracker.bot.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UntrackCommandTest {

    @Mock
    private LinkTrackingService linkTrackingService;

    @Mock
    private UserService userService;

    @Mock
    private BotMessagesService messages;

    @Mock
    private CommandContext context;

    @InjectMocks
    private UntrackCommand untrackCommand;

    @Test
    void command_returnsUntrack() {
        assertEquals("/untrack", untrackCommand.command());
    }

    @Test
    void description_returnsExpectedDescription() {
        assertEquals("Прекратить отслеживание ссылки", untrackCommand.description());
    }

    @Test
    void handle_whenUserNotRegistered_repliesChatNotRegistered() {
        when(context.chatId()).thenReturn(123L);
        when(userService.isRegistered(123L)).thenReturn(false);
        when(messages.chatNotRegistered()).thenReturn("Чат не зарегистрирован");

        untrackCommand.handle(context);

        verify(context).reply("Чат не зарегистрирован");
        verify(linkTrackingService, never()).removeLink(org.mockito.Mockito.anyLong(), org.mockito.Mockito.anyString());
    }

    @Test
    void handle_whenLinkNotProvided_repliesUsage() {
        when(context.chatId()).thenReturn(123L);
        when(context.messageText()).thenReturn("/untrack");
        when(userService.isRegistered(123L)).thenReturn(true);
        when(messages.untrackUsage()).thenReturn("Использование: /untrack <link>");

        untrackCommand.handle(context);

        verify(context).reply("Использование: /untrack <link>");
        verify(linkTrackingService, never()).removeLink(org.mockito.Mockito.anyLong(), org.mockito.Mockito.anyString());
    }

    @Test
    void handle_whenLinkIsBlank_repliesUsage() {
        when(context.chatId()).thenReturn(123L);
        when(context.messageText()).thenReturn("/untrack   ");
        when(userService.isRegistered(123L)).thenReturn(true);
        when(messages.untrackUsage()).thenReturn("Использование: /untrack <link>");

        untrackCommand.handle(context);

        verify(context).reply("Использование: /untrack <link>");
        verify(linkTrackingService, never()).removeLink(org.mockito.Mockito.anyLong(), org.mockito.Mockito.anyString());
    }

    @Test
    void handle_whenLinkRemoved_repliesSuccess() {
        when(context.chatId()).thenReturn(123L);
        when(context.messageText()).thenReturn("/untrack https://github.com/user/repo");
        when(userService.isRegistered(123L)).thenReturn(true);
        when(messages.linkRemoved()).thenReturn("Ссылка удалена");

        untrackCommand.handle(context);

        verify(linkTrackingService).removeLink(123L, "https://github.com/user/repo");
        verify(context).reply("Ссылка удалена");
    }

    @Test
    void handle_whenTrackedLinkNotFound_repliesLinkNotFound() {
        when(context.chatId()).thenReturn(123L);
        when(context.messageText()).thenReturn("/untrack https://github.com/user/repo");
        when(userService.isRegistered(123L)).thenReturn(true);
        when(messages.linkNotFound()).thenReturn("Ссылка не найдена");

        org.mockito.Mockito.doThrow(new TrackedLinkNotFoundException("not found"))
                .when(linkTrackingService)
                .removeLink(123L, "https://github.com/user/repo");

        untrackCommand.handle(context);

        verify(context).reply("Ссылка не найдена");
    }

    @Test
    void handle_whenChatNotRegisteredInScrapper_repliesChatNotRegistered() {
        when(context.chatId()).thenReturn(123L);
        when(context.messageText()).thenReturn("/untrack https://github.com/user/repo");
        when(userService.isRegistered(123L)).thenReturn(true);
        when(messages.chatNotRegistered()).thenReturn("Чат не зарегистрирован");

        org.mockito.Mockito.doThrow(new ChatNotRegisteredException("chat not registered"))
                .when(linkTrackingService)
                .removeLink(123L, "https://github.com/user/repo");

        untrackCommand.handle(context);

        verify(context).reply("Чат не зарегистрирован");
    }

    @Test
    void handle_trimsExtractedLink() {
        when(context.chatId()).thenReturn(123L);
        when(context.messageText()).thenReturn("/untrack    https://github.com/user/repo   ");
        when(userService.isRegistered(123L)).thenReturn(true);
        when(messages.linkRemoved()).thenReturn("Ссылка удалена");

        untrackCommand.handle(context);

        verify(linkTrackingService).removeLink(123L, "https://github.com/user/repo");
        verify(context).reply("Ссылка удалена");
    }
}
