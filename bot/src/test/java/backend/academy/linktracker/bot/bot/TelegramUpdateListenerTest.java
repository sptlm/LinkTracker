package backend.academy.linktracker.bot.bot;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandRegistry;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.TrackDialogService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramUpdateListenerTest {

    @Mock
    private TelegramBot bot;

    @Mock
    private CommandRegistry commandRegistry;

    @Mock
    private BotMessagesService messages;

    @Mock
    private TrackDialogService trackDialogService;

    @Mock
    private Command command;

    @Mock
    private Update update;

    @Mock
    private Message message;

    @Mock
    private Chat chat;

    private TelegramUpdateListener listener;

    @BeforeEach
    void setUp() {
        listener = new TelegramUpdateListener(bot, commandRegistry, messages, trackDialogService);

        lenient().when(update.message()).thenReturn(message);
        lenient().when(message.chat()).thenReturn(chat);
        lenient().when(chat.id()).thenReturn(777L);
    }

    @Test
    void shouldDispatchKnownCommand() {
        when(message.text()).thenReturn("/start anything");
        when(commandRegistry.find("/start")).thenReturn(Optional.of(command));

        listener.process(List.of(update));

        verify(command).handle(any());
        verify(bot, never()).execute(any(SendMessage.class));
    }

    @Test
    void shouldReplyOnUnknownCommand() {
        when(message.text()).thenReturn("/unknown arg");
        when(commandRegistry.find("/unknown")).thenReturn(Optional.empty());
        when(messages.unknownCommand())
                .thenReturn("Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд.");

        listener.process(List.of(update));

        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void shouldIgnoreNonCommandMessages() {
        when(message.text()).thenReturn("hello");

        listener.process(List.of(update));

        verify(commandRegistry, never()).find(any());
        verify(bot, never()).execute(any(SendMessage.class));
    }
}
