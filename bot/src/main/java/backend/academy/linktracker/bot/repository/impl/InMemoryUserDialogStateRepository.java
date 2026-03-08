package backend.academy.linktracker.bot.repository.impl;

import backend.academy.linktracker.bot.repository.UserDialogStateRepository;
import backend.academy.linktracker.bot.state.TrackDialogState;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUserDialogStateRepository implements UserDialogStateRepository {

    private final Map<Long, TrackDialogState> stateByChatId = new ConcurrentHashMap<>();

    @Override
    public Optional<TrackDialogState> findByChatId(long chatId) {
        return Optional.ofNullable(stateByChatId.get(chatId));
    }

    @Override
    public void save(long chatId, TrackDialogState state) {
        stateByChatId.put(chatId, state);
    }

    @Override
    public void delete(long chatId) {
        stateByChatId.remove(chatId);
    }
}
