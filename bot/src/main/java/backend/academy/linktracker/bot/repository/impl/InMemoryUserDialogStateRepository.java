package backend.academy.linktracker.bot.repository.impl;

import backend.academy.linktracker.bot.model.DialogSessionKey;
import backend.academy.linktracker.bot.repository.UserDialogStateRepository;
import backend.academy.linktracker.bot.state.TrackDialogState;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUserDialogStateRepository implements UserDialogStateRepository {

    private final Map<DialogSessionKey, TrackDialogState> statesBySessionKey = new ConcurrentHashMap<>();

    @Override
    public Optional<TrackDialogState> findById(DialogSessionKey sessionKey) {
        return Optional.ofNullable(statesBySessionKey.get(sessionKey));
    }

    @Override
    public void save(DialogSessionKey sessionKey, TrackDialogState state) {
        statesBySessionKey.put(sessionKey, state);
    }

    @Override
    public void delete(DialogSessionKey sessionKey) {
        statesBySessionKey.remove(sessionKey);
    }
}
