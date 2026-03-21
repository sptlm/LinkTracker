package backend.academy.linktracker.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.command.impl.ListCommand;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.LinkTrackingService;
import backend.academy.linktracker.bot.service.UserService;
import backend.academy.linktracker.scrapper.generated.dto.LinkResponse;
import backend.academy.linktracker.scrapper.generated.dto.ListLinksResponse;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
    private TelegramBot bot;

    @Mock
    private Message message;

    @Mock
    private Chat chat;

    @Captor
    private ArgumentCaptor<SendMessage> sendMessageCaptor;

    private ListCommand listCommand;

    @BeforeEach
    void setUp() {
        listCommand = new ListCommand(linkTrackingService, userService, messages);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
        when(message.from()).thenReturn(org.mockito.Mockito.mock(com.pengrad.telegrambot.model.User.class));
        when(message.from().id()).thenReturn(999L);
    }

    /**
     * Требование: Пользователь отправляет запрос /list, у него есть активные подписки.
     * Бот присылает пользователю список активных подписок.
     */
    @Test
    void shouldSendTrackedLinksList() {
        when(message.text()).thenReturn("/list");
        when(userService.isRegistered(999L)).thenReturn(true);
        when(linkTrackingService.getLinks(123L))
                .thenReturn(new ListLinksResponse()
                        .links(List.of(
                                new LinkResponse()
                                        .id(1L)
                                        .url(URI.create("https://github.com/user/repo"))
                                        .tags(List.of("java"))
                                        .filters(List.of("branch=main")),
                                new LinkResponse()
                                        .id(2L)
                                        .url(URI.create("https://stackoverflow.com/questions/123"))
                                        .tags(List.of("backend"))
                                        .filters(List.of())))
                        .size(2));

        listCommand.handle(new CommandContext(bot, message));

        verify(bot).execute(sendMessageCaptor.capture());
        Map<String, Object> parameters = sendMessageCaptor.getValue().getParameters();
        assertThat(parameters.get("chat_id")).isEqualTo(123L);
        assertThat(parameters.get("text"))
                .isEqualTo(
                        "https://github.com/user/repo\nТеги: java\nФильтры: branch=main\n\n"
                                + "https://stackoverflow.com/questions/123\nТеги: backend");
    }

    /**
     * Требование: Пользователь отправляет запрос /list, у него нет активных подписок.
     * Бот присылает пользователю сообщение о том, что у него нет активных подписок.
     */
    @Test
    void shouldSendNoTrackedLinksMessage() {
        when(message.text()).thenReturn("/list");
        when(userService.isRegistered(999L)).thenReturn(true);
        when(linkTrackingService.getLinks(123L)).thenReturn(new ListLinksResponse().links(List.of()).size(0));
        when(messages.noTrackedLinks()).thenReturn("У вас нет отслеживаемых ссылок.");

        listCommand.handle(new CommandContext(bot, message));

        verify(bot).execute(sendMessageCaptor.capture());
        assertThat(sendMessageCaptor.getValue().getParameters().get("text"))
                .isEqualTo("У вас нет отслеживаемых ссылок.");
    }

    /**
     * Требование: Пользователь отправляет запрос /list <tag>, у него есть активные подписки.
     * Бот присылает пользователю список активных подписок, имеющих тег <tag>.
     */
    @Test
    void shouldSendTrackedLinksFilteredByTag() {
        when(message.text()).thenReturn("/list java");
        when(userService.isRegistered(999L)).thenReturn(true);
        when(linkTrackingService.getLinks(123L))
                .thenReturn(new ListLinksResponse()
                        .links(List.of(
                                new LinkResponse()
                                        .id(1L)
                                        .url(URI.create("https://github.com/user/repo"))
                                        .tags(List.of("java"))
                                        .filters(List.of()),
                                new LinkResponse()
                                        .id(2L)
                                        .url(URI.create("https://stackoverflow.com/questions/123"))
                                        .tags(List.of("qa"))
                                        .filters(List.of())))
                        .size(2));

        listCommand.handle(new CommandContext(bot, message));

        verify(bot).execute(sendMessageCaptor.capture());
        assertThat(sendMessageCaptor.getValue().getParameters().get("text"))
                .isEqualTo("https://github.com/user/repo\nТеги: java");
    }
}
