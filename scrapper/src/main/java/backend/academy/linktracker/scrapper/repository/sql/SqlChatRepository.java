package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "access-type", havingValue = "SQL", matchIfMissing = true)
public class SqlChatRepository implements ChatRepository {

    private final JdbcClient jdbcClient;

    public SqlChatRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean exists(long chatId) {
        Long count = jdbcClient
                .sql("select count(*) from tg_chat where chat_id = :chatId")
                .param("chatId", chatId)
                .query(Long.class)
                .single();
        return count != null && count > 0;
    }

    @Override
    public void save(long chatId) {
        jdbcClient
                .sql("insert into tg_chat (chat_id) values (:chatId)")
                .param("chatId", chatId)
                .update();
    }

    @Override
    public void delete(long chatId) {
        jdbcClient
                .sql("delete from tg_chat where chat_id = :chatId")
                .param("chatId", chatId)
                .update();
    }
}
