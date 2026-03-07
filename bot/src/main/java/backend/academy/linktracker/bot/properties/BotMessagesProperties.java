package backend.academy.linktracker.bot.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.messages")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class BotMessagesProperties {

    @NotBlank
    private String unknownCommand =
            "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд.";

    @NotBlank
    private String startWelcome = "Добро пожаловать, %s! Используйте /help, чтобы посмотреть доступные команды.";

    @NotBlank
    private String welcomeBack = "С возвращением, %s! Используйте /help, чтобы посмотреть доступные команды.";

    @NotBlank
    private String helpHeader = "Доступные команды:";
}
