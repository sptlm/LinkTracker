package backend.academy.linktracker.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.command.impl.HelpCommand;
import backend.academy.linktracker.bot.service.BotMessagesService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
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
class HelpCommandTest {

    @Mock
    private CommandRegistry commandRegistry;

    @Mock
    private BotMessagesService messages;

    @Mock
    private TelegramBot bot;

    @Mock
    private Message message;

    @Mock
    private Chat chat;

    @Mock
    private Command startCommand;

    @Mock
    private Command helpMetaCommand;

    @Captor
    private ArgumentCaptor<SendMessage> sendMessageCaptor;

    private HelpCommand helpCommand;

    @BeforeEach
    void setUp() {
        helpCommand = new HelpCommand(commandRegistry, messages);

        lenient().when(message.chat()).thenReturn(chat);
        lenient().when(chat.id()).thenReturn(321L);

        lenient().when(startCommand.command()).thenReturn("/start");
        lenient().when(startCommand.description()).thenReturn("Начало работы с ботом");

        lenient().when(helpMetaCommand.command()).thenReturn("/help");
        lenient().when(helpMetaCommand.description()).thenReturn("Список доступных команд");
    }

    @Test
    void shouldSendFormattedCommandsList() {
        when(messages.helpHeader()).thenReturn("Доступные команды:");
        when(commandRegistry.getCommands()).thenReturn(List.of(helpMetaCommand, startCommand));

        helpCommand.handle(new CommandContext(bot, message));

        verify(messages).helpHeader();
        verify(bot).execute(sendMessageCaptor.capture());

        Map<String, Object> parameters = sendMessageCaptor.getValue().getParameters();
        assertThat(parameters.get("chat_id")).isEqualTo(321L);
        assertThat(parameters.get("text")).isEqualTo("""
                        Доступные команды:
                        /help — Список доступных команд
                        /start — Начало работы с ботом""");
    }

    @Test
    void shouldSendOnlyHeaderWhenNoCommands() {
        when(messages.helpHeader()).thenReturn("Доступные команды:");
        when(commandRegistry.getCommands()).thenReturn(List.of());

        helpCommand.handle(new CommandContext(bot, message));

        verify(messages).helpHeader();
        verify(bot).execute(sendMessageCaptor.capture());

        Map<String, Object> parameters = sendMessageCaptor.getValue().getParameters();
        assertThat(parameters.get("chat_id")).isEqualTo(321L);
        assertThat(parameters.get("text")).isEqualTo("Доступные команды:\n");
    }
}
