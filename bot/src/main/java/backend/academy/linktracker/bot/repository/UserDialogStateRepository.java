package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.state.TrackDialogState;
import java.util.Optional;

public interface UserDialogStateRepository {

    Optional<TrackDialogState> findByChatId(long chatId);

    void save(long chatId, TrackDialogState state);

    void delete(long chatId);
}
