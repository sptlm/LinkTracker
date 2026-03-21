package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.DialogSessionKey;
import backend.academy.linktracker.bot.state.TrackDialogState;
import java.util.Optional;

public interface UserDialogStateRepository {

    Optional<TrackDialogState> findById(DialogSessionKey sessionKey);

    void save(DialogSessionKey sessionKey, TrackDialogState state);

    void delete(DialogSessionKey sessionKey);
}
