package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.User;
import java.util.Optional;

public interface UserRepository {

    void save(User user);

    boolean existsByChatId(long chatId);

    Optional<User> findByChatId(long chatId);
}
