package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.TgChatEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.TgChatJpaRepository;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "access-type", havingValue = "ORM")
public class OrmChatRepository implements ChatRepository {

    private final TgChatJpaRepository chatJpaRepository;

    public OrmChatRepository(TgChatJpaRepository chatJpaRepository) {
        this.chatJpaRepository = chatJpaRepository;
    }

    @Override
    public boolean exists(long chatId) {
        return chatJpaRepository.existsById(chatId);
    }

    @Override
    public void save(long chatId) {
        chatJpaRepository.save(new TgChatEntity(chatId, Instant.now()));
    }

    @Override
    public void delete(long chatId) {
        chatJpaRepository.deleteById(chatId);
    }
}
