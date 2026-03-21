package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.User;
import java.util.Optional;

public interface UserRepository {

    void save(User user);

    boolean existsByUserId(long userId);

    Optional<User> findByUserId(long userId);
}
