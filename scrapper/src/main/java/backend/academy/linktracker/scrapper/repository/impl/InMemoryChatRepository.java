package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryChatRepository implements ChatRepository {

    private final Set<Long> chats = ConcurrentHashMap.newKeySet();

    @Override
    public boolean exists(long chatId) {
        return chats.contains(chatId);
    }

    @Override
    public void save(long chatId) {
        chats.add(chatId);
    }

    @Override
    public void delete(long chatId) {
        chats.remove(chatId);
    }
}
