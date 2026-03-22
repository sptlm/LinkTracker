package backend.academy.linktracker.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.command.impl.StartCommand;
import backend.academy.linktracker.bot.service.BotMessagesService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StartCommandTest {

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

    private StartCommand startCommand;

    @BeforeEach
    void setUp() {
        startCommand = new StartCommand(messages);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
        when(message.from()).thenReturn(org.mockito.Mockito.mock(com.pengrad.telegrambot.model.User.class));
    }

    @Test
    void shouldSendWelcomeMessageWithFirstName() {
        when(message.from().firstName()).thenReturn("Ivan");
        when(messages.startWelcome("Ivan")).thenReturn("Добро пожаловать, Ivan!");

        startCommand.handle(new CommandContext(bot, message));

        verify(messages).startWelcome("Ivan");
        verify(bot).execute(sendMessageCaptor.capture());

        Map<String, Object> parameters = sendMessageCaptor.getValue().getParameters();
        assertThat(parameters.get("chat_id")).isEqualTo(123L);
        assertThat(parameters.get("text")).isEqualTo("Добро пожаловать, Ivan!");
    }

    @Test
    void shouldFallbackToUsernameWhenFirstNameMissing() {
        when(message.from().firstName()).thenReturn(null);
        when(message.from().username()).thenReturn("ivanov");
        when(messages.startWelcome("@ivanov")).thenReturn("Добро пожаловать, @ivanov!");

        startCommand.handle(new CommandContext(bot, message));

        verify(messages).startWelcome("@ivanov");
        verify(bot).execute(sendMessageCaptor.capture());
        assertThat(sendMessageCaptor.getValue().getParameters().get("text")).isEqualTo("Добро пожаловать, @ivanov!");
    }
}
