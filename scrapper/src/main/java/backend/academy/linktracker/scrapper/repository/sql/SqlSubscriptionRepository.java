package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.LinkSubscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "access-type", havingValue = "SQL", matchIfMissing = true)
public class SqlSubscriptionRepository implements SubscriptionRepository {

    private static final RowMapper<LinkSubscription> SUBSCRIPTION_ROW_MAPPER = SqlSubscriptionRepository::mapRow;

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public SqlSubscriptionRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
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
        long subscriptionId = insertSubscription(subscription);
        batchInsertTags(subscriptionId, subscription.tags());
        return subscription;
    }

    @Override
    public Optional<LinkSubscription> findByChatIdAndLinkId(long chatId, long linkId) {
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
                  on st.subscription_id = s.id
                """;
    }

    private static LinkSubscription mapRow(ResultSet rs, int ignoredRowNum) throws SQLException {
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

    private long insertSubscription(LinkSubscription subscription) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        PreparedStatementCreatorFactory pscFactory = new PreparedStatementCreatorFactory(
                "insert into link_subscription (chat_id, link_id, created_at) values (?, ?, ?)");
        pscFactory.setGeneratedKeysColumnNames("id");
        jdbcTemplate.update(
                pscFactory.newPreparedStatementCreator(List.of(
                        subscription.chatId(), subscription.linkId(), toOffsetDateTime(subscription.createdAt()))),
                keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to insert link_subscription and retrieve generated id");
        }
        return key.longValue();
    }

    private void batchInsertTags(long subscriptionId, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                "insert into subscription_tag (subscription_id, tag) values (?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
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
