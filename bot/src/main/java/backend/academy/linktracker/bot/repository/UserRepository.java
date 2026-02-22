package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.User;
import java.util.Optional;

public interface UserRepository {

    boolean existsById(long chatId);

    void save(User user);

    Optional<User> findById(long chatId);
}
