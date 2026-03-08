package backend.academy.linktracker.scrapper.repository;

public interface ChatRepository {

    boolean exists(long chatId);

    void save(long chatId);

    void delete(long chatId);
}
