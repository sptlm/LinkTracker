package backend.academy.linktracker.bot.command.impl;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.model.User;
import backend.academy.linktracker.bot.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommand implements Command {

    private final UserRepository userRepository;

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public String description() {
        return "Начало работы с ботом";
    }

    @Override
    public String handle(CommandContext context) {
        if (!userRepository.existsById(context.chatId())) {
            // Строим модель пользователя из данных Telegram
            User user = User.builder()
                    .chatId(context.chatId())
                    .username(context.username())
                    .firstName(context.firstName())
                    .lastName(context.lastName())
                    .registeredAt(Instant.now())
                    .build();

            userRepository.save(user);

            return "Добро пожаловать, %s! Используйте /help, чтобы посмотреть доступные команды."
                    .formatted(user.displayName());
        }

        // Пользователь уже зарегистрирован — приветствуем по имени
        return userRepository
                .findById(context.chatId())
                .map(user -> "С возвращением, %s! Используйте /help, чтобы посмотреть доступные команды."
                        .formatted(user.displayName()))
                .orElse("С возвращением! Используйте /help, чтобы посмотреть доступные команды.");
    }
}
