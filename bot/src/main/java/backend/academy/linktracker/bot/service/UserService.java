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
        long chatId = message.chat().id();

        return userRepository.findByChatId(chatId)
            .map(user -> new RegistrationResult(user, false))
            .orElseGet(() -> {
                var from = message.from();

                User user = User.builder()
                    .chatId(chatId)
                    .username(from != null ? from.username() : null)
                    .firstName(from != null ? from.firstName() : null)
                    .lastName(from != null ? from.lastName() : null)
                    .registeredAt(Instant.now())
                    .build();

                userRepository.save(user);
                return new RegistrationResult(user, true);
            });
    }

    public boolean isRegistered(long chatId) {
        return userRepository.existsByChatId(chatId);
    }
}
