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

    @NotBlank
    private String trackStarted = "Пришлите ссылку, которую хотите отслеживать.";

    @NotBlank
    private String trackAskTags = "Теперь пришлите теги через запятую или отправьте '-' если теги не нужны.";


    @NotBlank
    private String trackCancelled = "Отслеживание ссылки отменено.";

    @NotBlank
    private String nothingToCancel = "Сейчас нечего отменять.";

    @NotBlank
    private String invalidLink = "Некорректная ссылка.";

    @NotBlank
    private String alreadyTracked = "Ссылка уже отслеживается";

    @NotBlank
    private String linkTracked = "Ссылка добавлена в отслеживание.";

    @NotBlank
    private String linkRemoved = "Ссылка удалена из отслеживания.";

    @NotBlank
    private String linkNotFound = "Ссылка не найдена.";

    @NotBlank
    private String noTrackedLinks = "У вас нет отслеживаемых ссылок.";

    @NotBlank
    private String chatNotRegistered = "Сначала используйте /start.";

    @NotBlank
    private String untrackUsage = "Использование: /untrack <ссылка>";

    @NotBlank
    private String updatesTemplate = "Обнаружено обновление по ссылке: %s%n%s";

    @NotBlank
    private String scrapperUnavailable = "Сервис отслеживания сейчас недоступен. Попробуйте позже.";
}
