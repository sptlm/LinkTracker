package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.model.User;
import backend.academy.linktracker.bot.repository.UserRepository;
import com.pengrad.telegrambot.model.Message;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public RegistrationResult registerIfAbsent(Message message) {
        com.pengrad.telegrambot.model.User telegramUser = requireSender(message);
        long userId = telegramUser.id();

        return userRepository
            .findByUserId(userId)
            .map(user -> new RegistrationResult(user, false))
            .orElseGet(() -> {
                User user = User.builder()
                    .userId(userId)
                    .username(telegramUser.username())
                    .firstName(telegramUser.firstName())
                    .lastName(telegramUser.lastName())
                    .registeredAt(Instant.now())
                    .build();

                userRepository.save(user);
                return new RegistrationResult(user, true);
            });
    }

    public boolean isRegistered(long userId) {
        return userRepository.existsByUserId(userId);
    }

    private com.pengrad.telegrambot.model.User requireSender(Message message) {
        if (message.from() == null) {
            throw new IllegalArgumentException("Telegram update does not contain sender information");
        }
        return message.from();
    }
}
