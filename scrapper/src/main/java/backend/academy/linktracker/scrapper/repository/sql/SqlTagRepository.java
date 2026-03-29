package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.repository.TagRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "access-type", havingValue = "SQL", matchIfMissing = true)
public class SqlTagRepository implements TagRepository {

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public SqlTagRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean exists(long chatId, long linkId, String tag) {
        Long count = jdbcClient
                .sql(
                        """
                        select count(*)
                        from subscription_tag st
                        join link_subscription ls on ls.id = st.subscription_id
                        where ls.chat_id = :chatId and ls.link_id = :linkId and st.tag = :tag
                        """)
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
                        select st.tag
                        from subscription_tag st
                        join link_subscription ls on ls.id = st.subscription_id
                        where ls.chat_id = :chatId and ls.link_id = :linkId
                        order by st.tag
                        """)
                .param("chatId", chatId)
                .param("linkId", linkId)
                .query(String.class)
                .list();
    }

    @Override
    public void add(long chatId, long linkId, String tag) {
        jdbcClient
                .sql(
                        """
                        insert into subscription_tag (subscription_id, tag)
                        select id, :tag
                        from link_subscription
                        where chat_id = :chatId and link_id = :linkId
                        """)
                .param("chatId", chatId)
                .param("linkId", linkId)
                .param("tag", tag)
                .update();
    }

    @Override
    public void replace(long chatId, long linkId, List<String> tags) {
        Long subscriptionId = jdbcClient
                .sql("select id from link_subscription where chat_id = :chatId and link_id = :linkId")
                .param("chatId", chatId)
                .param("linkId", linkId)
                .query(Long.class)
                .optional()
                .orElse(null);

        if (subscriptionId == null) {
            return;
        }

        jdbcClient
                .sql("delete from subscription_tag where subscription_id = :subscriptionId")
                .param("subscriptionId", subscriptionId)
                .update();

        batchInsert(subscriptionId, tags);
    }

    @Override
    public void delete(long chatId, long linkId, String tag) {
        jdbcClient
                .sql(
                        """
                        delete from subscription_tag st
                        using link_subscription ls
                        where st.subscription_id = ls.id
                          and ls.chat_id = :chatId
                          and ls.link_id = :linkId
                          and st.tag = :tag
                        """)
                .param("chatId", chatId)
                .param("linkId", linkId)
                .param("tag", tag)
                .update();
    }

    private void batchInsert(long subscriptionId, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                "insert into subscription_tag (subscription_id, tag) values (?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                        ps.setLong(1, subscriptionId);
                        ps.setString(2, tags.get(i));
                    }

                    @Override
                    public int getBatchSize() {
                        return tags.size();
                    }
                });
    }
}
