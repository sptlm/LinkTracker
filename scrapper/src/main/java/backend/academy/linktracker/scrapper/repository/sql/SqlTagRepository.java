package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.repository.TagRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "access-type", havingValue = "SQL", matchIfMissing = true)
public class SqlTagRepository implements TagRepository {

    private final JdbcClient jdbcClient;

    public SqlTagRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean exists(long chatId, long linkId, String tag) {
        Long count = jdbcClient
                .sql(
                        "select count(*) from subscription_tag where chat_id = :chatId and link_id = :linkId and tag = :tag")
                .param("chatId", chatId)
                .param("linkId", linkId)
                .param("tag", tag)
                .query(Long.class)
                .single();
        return count != null && count > 0;
    }

    @Override
    public List<String> findByChatIdAndLinkId(long chatId, long linkId) {
        return jdbcClient
                .sql("""
                        select tag
                        from subscription_tag
                        where chat_id = :chatId and link_id = :linkId
                        order by tag
                        """)
                .param("chatId", chatId)
                .param("linkId", linkId)
                .query(String.class)
                .list();
    }

    @Override
    public void add(long chatId, long linkId, String tag) {
        jdbcClient
                .sql("insert into subscription_tag (chat_id, link_id, tag) values (:chatId, :linkId, :tag)")
                .param("chatId", chatId)
                .param("linkId", linkId)
                .param("tag", tag)
                .update();
    }

    @Override
    public void replace(long chatId, long linkId, List<String> tags) {
        jdbcClient
                .sql("delete from subscription_tag where chat_id = :chatId and link_id = :linkId")
                .param("chatId", chatId)
                .param("linkId", linkId)
                .update();

        for (String tag : tags) {
            add(chatId, linkId, tag);
        }
    }

    @Override
    public void delete(long chatId, long linkId, String tag) {
        jdbcClient
                .sql("delete from subscription_tag where chat_id = :chatId and link_id = :linkId and tag = :tag")
                .param("chatId", chatId)
                .param("linkId", linkId)
                .param("tag", tag)
                .update();
    }
}
