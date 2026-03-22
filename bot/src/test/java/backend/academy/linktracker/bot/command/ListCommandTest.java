package backend.academy.linktracker.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.command.impl.ListCommand;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.LinkTrackingService;
import backend.academy.linktracker.scrapper.generated.dto.LinksPost200Response;
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
        listCommand = new ListCommand(linkTrackingService, messages);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
    }

    @Test
    void shouldSendTrackedLinksList() {
        when(message.text()).thenReturn("/list");
        when(linkTrackingService.getLinks(123L))
                .thenReturn(new ListLinksResponse()
                        .links(List.of(
                                new LinksPost200Response()
                                        .id(1L)
                                        .url(URI.create("https://github.com/user/repo"))
                                        .tags(List.of("java"))
                                        .filters(List.of()),
                                new LinksPost200Response()
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
                .isEqualTo("https://github.com/user/repo\nТеги: java\n\n"
                        + "https://stackoverflow.com/questions/123\nТеги: backend");
    }

    @Test
    void shouldSendNoTrackedLinksMessage() {
        when(message.text()).thenReturn("/list");
        when(linkTrackingService.getLinks(123L))
                .thenReturn(new ListLinksResponse().links(List.of()).size(0));
        when(messages.noTrackedLinks()).thenReturn("У вас нет отслеживаемых ссылок.");

        listCommand.handle(new CommandContext(bot, message));

        verify(bot).execute(sendMessageCaptor.capture());
        assertThat(sendMessageCaptor.getValue().getParameters().get("text"))
                .isEqualTo("У вас нет отслеживаемых ссылок.");
    }

    @Test
    void shouldSendTrackedLinksFilteredByTag() {
        when(message.text()).thenReturn("/list java");
        when(linkTrackingService.getLinks(123L))
                .thenReturn(new ListLinksResponse()
                        .links(List.of(
                                new LinksPost200Response()
                                        .id(1L)
                                        .url(URI.create("https://github.com/user/repo"))
                                        .tags(List.of("java"))
                                        .filters(List.of()),
                                new LinksPost200Response()
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
