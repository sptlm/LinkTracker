package backend.academy.linktracker.bot.bot;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandRegistry;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pengrad.telegrambot.response.BaseResponse;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final TelegramBot bot;
    private final CommandRegistry commandRegistry;
    private final TelegramUpdateListener updateListener;

    @PostConstruct
    public void start() {
        registerCommandsMenu();
        startListening();
    }

    private void registerCommandsMenu() {
        List<Command> commands = commandRegistry.getCommands();

        BotCommand[] botCommands = commands.stream()
                .map(cmd -> new BotCommand(cmd.command(), cmd.description()))
                .toArray(BotCommand[]::new);

        try {
            BaseResponse response = bot.execute(new SetMyCommands(botCommands));
            if (response.isOk()) {
                log.atInfo()
                        .addKeyValue("commandCount", botCommands.length)
                        .log("Bot commands successfully registered in Telegram menu");
            } else {
                log.atWarn()
                        .addKeyValue("errorCode", response.errorCode())
                        .addKeyValue("description", response.description())
                        .log("Failed to register bot commands in Telegram menu");
            }
        } catch (Exception e) {
            log.atWarn().setCause(e).log("Exception while registering bot commands menu, skipping");
        }
    }

    private void startListening() {
        bot.setUpdatesListener(
                updateListener,
                // Обработчик ошибок
                e -> {
                    if (e.response() != null) {
                        log.atError()
                                .addKeyValue("errorCode", e.response().errorCode())
                                .addKeyValue("description", e.response().description())
                                .log("Telegram API error during updates polling");
                    } else {
                        log.atError().setCause(e).log("Network error during updates polling");
                    }
                });

        log.atInfo().log("Telegram bot started and listening for updates");
    }
}
