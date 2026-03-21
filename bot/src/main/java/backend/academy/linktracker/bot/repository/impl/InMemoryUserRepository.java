package backend.academy.linktracker.bot.repository.impl;

import backend.academy.linktracker.bot.model.User;
import backend.academy.linktracker.bot.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> usersById = new ConcurrentHashMap<>();

    @Override
    public boolean existsByUserId(long userId) {
        return usersById.containsKey(userId);
    }

    @Override
    public void save(User user) {
        usersById.put(user.getUserId(), user);
    }

    @Override
    public Optional<User> findByUserId(long userId) {
        return Optional.ofNullable(usersById.get(userId));
    }
}
