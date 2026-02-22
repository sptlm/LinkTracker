package backend.academy.linktracker.bot.repository.impl;

import backend.academy.linktracker.bot.model.User;
import backend.academy.linktracker.bot.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> users = new ConcurrentHashMap<>();

    @Override
    public boolean existsById(long chatId) {
        return users.containsKey(chatId);
    }

    @Override
    public void save(User user) {
        users.put(user.getChatId(), user);
    }

    @Override
    public Optional<User> findById(long chatId) {
        return Optional.ofNullable(users.get(chatId));
    }
}
