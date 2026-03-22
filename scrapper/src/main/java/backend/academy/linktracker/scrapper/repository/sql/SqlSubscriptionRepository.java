package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.LinkSubscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "access-type", havingValue = "SQL", matchIfMissing = true)
public class SqlSubscriptionRepository implements SubscriptionRepository {

    private static final RowMapper<LinkSubscription> SUBSCRIPTION_ROW_MAPPER = SqlSubscriptionRepository::mapRow;

    private final JdbcClient jdbcClient;

    public SqlSubscriptionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean exists(long chatId, long linkId) {
        Long count = jdbcClient
                .sql("select count(*) from link_subscription where chat_id = :chatId and link_id = :linkId")
                .param("chatId", chatId)
                .param("linkId", linkId)
                .query(Long.class)
                .single();
        return count != null && count > 0;
    }

    @Override
    public LinkSubscription save(LinkSubscription subscription) {
        jdbcClient
                .sql(
                        "insert into link_subscription (chat_id, link_id, created_at) values (:chatId, :linkId, :createdAt)")
                .param("chatId", subscription.chatId())
                .param("linkId", subscription.linkId())
                .param("createdAt", toOffsetDateTime(subscription.createdAt()))
                .update();

        for (String tag : subscription.tags()) {
            jdbcClient
                    .sql("insert into subscription_tag (chat_id, link_id, tag) values (:chatId, :linkId, :tag)")
                    .param("chatId", subscription.chatId())
                    .param("linkId", subscription.linkId())
                    .param("tag", tag)
                    .update();
        }

        return subscription;
    }

    @Override
    public java.util.Optional<LinkSubscription> findByChatIdAndLinkId(long chatId, long linkId) {
        return jdbcClient
                .sql(
                        baseQuery()
                                + " where s.chat_id = :chatId and s.link_id = :linkId group by s.chat_id, s.link_id, s.created_at")
                .param("chatId", chatId)
                .param("linkId", linkId)
                .query(SUBSCRIPTION_ROW_MAPPER)
                .optional();
    }

    @Override
    public List<LinkSubscription> findByChatId(long chatId) {
        return jdbcClient
                .sql(baseQuery()
                        + " where s.chat_id = :chatId group by s.chat_id, s.link_id, s.created_at order by s.link_id")
                .param("chatId", chatId)
                .query(SUBSCRIPTION_ROW_MAPPER)
                .list();
    }

    @Override
    public List<LinkSubscription> findByLinkId(long linkId) {
        return jdbcClient
                .sql(baseQuery()
                        + " where s.link_id = :linkId group by s.chat_id, s.link_id, s.created_at order by s.chat_id")
                .param("linkId", linkId)
                .query(SUBSCRIPTION_ROW_MAPPER)
                .list();
    }

    @Override
    public List<Long> findChatIdsByLinkId(long linkId) {
        return jdbcClient
                .sql("select chat_id from link_subscription where link_id = :linkId order by chat_id")
                .param("linkId", linkId)
                .query(Long.class)
                .list();
    }

    @Override
    public void delete(long chatId, long linkId) {
        jdbcClient
                .sql("delete from link_subscription where chat_id = :chatId and link_id = :linkId")
                .param("chatId", chatId)
                .param("linkId", linkId)
                .update();
    }

    @Override
    public boolean hasSubscribers(long linkId) {
        Long count = jdbcClient
                .sql("select count(*) from link_subscription where link_id = :linkId")
                .param("linkId", linkId)
                .query(Long.class)
                .single();
        return count != null && count > 0;
    }

    private static String baseQuery() {
        return """
                select s.chat_id,
                       s.link_id,
                       s.created_at,
                       coalesce(array_agg(st.tag order by st.tag) filter (where st.tag is not null), '{}') as tags
                from link_subscription s
                left join subscription_tag st
                  on st.chat_id = s.chat_id
                 and st.link_id = s.link_id
                """;
    }

    private static LinkSubscription mapRow(ResultSet rs, int rowNum) throws SQLException {
        Array tagsArray = rs.getArray("tags");
        List<String> tags = tagsArray == null ? List.of() : Arrays.asList((String[]) tagsArray.getArray());

        return new LinkSubscription(
                rs.getLong("chat_id"),
                rs.getLong("link_id"),
                tags,
                rs.getTimestamp("created_at").toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
