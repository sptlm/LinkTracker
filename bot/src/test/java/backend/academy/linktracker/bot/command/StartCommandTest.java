package backend.academy.linktracker.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.command.impl.StartCommand;
import backend.academy.linktracker.bot.model.User;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.LinkTrackingService;
import backend.academy.linktracker.bot.service.RegistrationResult;
import backend.academy.linktracker.bot.service.UserService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import java.time.Instant;
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
    private UserService userService;

    @Mock
    private BotMessagesService messages;

    @Mock
    private TelegramBot bot;

    @Mock
    private Message message;

    @Mock
    private Chat chat;

    @Mock
    private LinkTrackingService linkTrackingService;

    @Captor
    private ArgumentCaptor<SendMessage> sendMessageCaptor;

    private StartCommand startCommand;

    @BeforeEach
    void setUp() {
        startCommand = new StartCommand(userService,linkTrackingService, messages);

        lenient().when(message.chat()).thenReturn(chat);
        lenient().when(chat.id()).thenReturn(123L);
    }

    @Test
    void shouldSendWelcomeMessageForNewUser() {
        User user = User.builder()
                .chatId(123L)
                .firstName("Ivan")
                .registeredAt(Instant.now())
                .build();

        when(userService.registerIfAbsent(message)).thenReturn(new RegistrationResult(user, true));
        when(messages.startWelcome("Ivan"))
                .thenReturn("Добро пожаловать, Ivan! Используйте /help, чтобы посмотреть доступные команды.");

        startCommand.handle(new CommandContext(bot, message));

        verify(messages).startWelcome("Ivan");
        verify(bot).execute(sendMessageCaptor.capture());

        Map<String, Object> parameters = sendMessageCaptor.getValue().getParameters();
        assertThat(parameters.get("chat_id")).isEqualTo(123L);
        assertThat(parameters.get("text"))
                .isEqualTo("Добро пожаловать, Ivan! Используйте /help, чтобы посмотреть доступные команды.");
    }

    @Test
    void shouldSendWelcomeBackMessageForExistingUser() {
        User user = User.builder()
                .chatId(123L)
                .firstName("Ivan")
                .registeredAt(Instant.now())
                .build();

        when(userService.registerIfAbsent(message)).thenReturn(new RegistrationResult(user, false));
        when(messages.welcomeBack("Ivan"))
                .thenReturn("С возвращением, Ivan! Используйте /help, чтобы посмотреть доступные команды.");

        startCommand.handle(new CommandContext(bot, message));

        verify(messages).welcomeBack("Ivan");
        verify(bot).execute(sendMessageCaptor.capture());

        Map<String, Object> parameters = sendMessageCaptor.getValue().getParameters();
        assertThat(parameters.get("chat_id")).isEqualTo(123L);
        assertThat(parameters.get("text"))
                .isEqualTo("С возвращением, Ivan! Используйте /help, чтобы посмотреть доступные команды.");
    }
}
