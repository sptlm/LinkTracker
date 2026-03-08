package backend.academy.linktracker.bot.command;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.scrapper.ChatNotRegisteredException;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClientException;
import backend.academy.linktracker.bot.client.scrapper.dto.LinkResponse;
import backend.academy.linktracker.bot.client.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.bot.command.impl.ListCommand;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.LinkTrackingService;
import backend.academy.linktracker.bot.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListCommandTest {

    @Mock
    private LinkTrackingService linkTrackingService;

    @Mock
    private UserService userService;

    @Mock
    private BotMessagesService messages;

    @Mock
    private CommandContext context;

    @InjectMocks
    private ListCommand listCommand;

    @Test
    void handle_whenUserNotRegistered_repliesChatNotRegistered() {
        when(context.chatId()).thenReturn(123L);
        when(userService.isRegistered(123L)).thenReturn(false);
        when(messages.chatNotRegistered()).thenReturn("Чат не зарегистрирован");

        listCommand.handle(context);

        verify(context).reply("Чат не зарегистрирован");
        verify(linkTrackingService, never()).getLinks(123L);
    }

    @Test
    void handle_whenNoLinks_repliesNoTrackedLinks() {
        ListLinksResponse response = org.mockito.Mockito.mock(ListLinksResponse.class);

        when(context.chatId()).thenReturn(123L);
        when(context.messageText()).thenReturn("/list");
        when(userService.isRegistered(123L)).thenReturn(true);
        when(linkTrackingService.getLinks(123L)).thenReturn(response);
        when(response.links()).thenReturn(List.of());
        when(messages.noTrackedLinks()).thenReturn("Нет отслеживаемых ссылок");

        listCommand.handle(context);

        verify(context).reply("Нет отслеживаемых ссылок");
    }

    @Test
    void handle_whenResponseLinksIsNull_repliesNoTrackedLinks() {
        ListLinksResponse response = org.mockito.Mockito.mock(ListLinksResponse.class);

        when(context.chatId()).thenReturn(123L);
        when(context.messageText()).thenReturn("/list");
        when(userService.isRegistered(123L)).thenReturn(true);
        when(linkTrackingService.getLinks(123L)).thenReturn(response);
        when(response.links()).thenReturn(null);
        when(messages.noTrackedLinks()).thenReturn("Нет отслеживаемых ссылок");

        listCommand.handle(context);

        verify(context).reply("Нет отслеживаемых ссылок");
    }

    @Test
    void handle_whenLinksExist_repliesFormattedLinks() {
        ListLinksResponse response = org.mockito.Mockito.mock(ListLinksResponse.class);
        LinkResponse first = org.mockito.Mockito.mock(LinkResponse.class);
        LinkResponse second = org.mockito.Mockito.mock(LinkResponse.class);

        when(context.chatId()).thenReturn(123L);
        when(context.messageText()).thenReturn("/list");
        when(userService.isRegistered(123L)).thenReturn(true);
        when(linkTrackingService.getLinks(123L)).thenReturn(response);
        when(response.links()).thenReturn(List.of(first, second));

        when(first.url()).thenReturn("https://github.com/user/repo");
        when(first.tags()).thenReturn(List.of("java", "spring"));
        when(first.filters()).thenReturn(List.of("branch=main"));

        when(second.url()).thenReturn("https://stackoverflow.com/questions/123");
        when(second.tags()).thenReturn(List.of());
        when(second.filters()).thenReturn(List.of());

        listCommand.handle(context);

        verify(context)
                .reply("https://github.com/user/repo\n" + "Теги: java, spring\n"
                        + "Фильтры: branch=main\n\n"
                        + "https://stackoverflow.com/questions/123");
    }

    @Test
    void handle_whenTagProvidedAndNothingMatches_repliesNoTrackedLinks() {
        ListLinksResponse response = org.mockito.Mockito.mock(ListLinksResponse.class);
        LinkResponse sqlLink = org.mockito.Mockito.mock(LinkResponse.class);

        when(context.chatId()).thenReturn(123L);
        when(context.messageText()).thenReturn("/list java");
        when(userService.isRegistered(123L)).thenReturn(true);
        when(linkTrackingService.getLinks(123L)).thenReturn(response);
        when(response.links()).thenReturn(List.of(sqlLink));
        when(sqlLink.tags()).thenReturn(List.of("sql"));
        when(messages.noTrackedLinks()).thenReturn("Нет отслеживаемых ссылок");

        listCommand.handle(context);

        verify(context).reply("Нет отслеживаемых ссылок");
    }

    @Test
    void handle_whenChatNotRegisteredException_repliesChatNotRegistered() {
        when(context.chatId()).thenReturn(123L);
        when(context.messageText()).thenReturn("/list");
        when(userService.isRegistered(123L)).thenReturn(true);
        when(messages.chatNotRegistered()).thenReturn("Чат не зарегистрирован");
        when(linkTrackingService.getLinks(123L)).thenThrow(new ChatNotRegisteredException("chat not registered"));

        listCommand.handle(context);

        verify(context).reply("Чат не зарегистрирован");
    }

    @Test
    void handle_whenScrapperUnavailable_repliesScrapperUnavailable() {
        when(context.chatId()).thenReturn(123L);
        when(context.messageText()).thenReturn("/list");
        when(userService.isRegistered(123L)).thenReturn(true);
        when(messages.scrapperUnavailable()).thenReturn("Scrapper недоступен");
        when(linkTrackingService.getLinks(123L)).thenThrow(new ScrapperClientException("scrapper unavailable"));

        listCommand.handle(context);

        verify(context).reply("Scrapper недоступен");
    }
}
