package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "access-type", havingValue = "SQL", matchIfMissing = true)
public class SqlLinkRepository implements LinkRepository {

    private static final RowMapper<TrackedLink> LINK_ROW_MAPPER = SqlLinkRepository::mapRow;

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public SqlLinkRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TrackedLink save(TrackedLink link) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    java.sql.PreparedStatement ps = connection.prepareStatement(
                            "insert into tracked_link (url, type, created_at, last_checked_at, last_updated_at) values (?, ?, ?, ?, ?)",
                            new String[] {"id"});
                    ps.setString(1, link.url());
                    ps.setString(2, link.type().name());
                    ps.setObject(3, toOffsetDateTime(link.createdAt()));
                    ps.setObject(4, toOffsetDateTime(link.lastCheckedAt()));
                    ps.setObject(5, toOffsetDateTime(link.lastUpdatedAt()));
                    return ps;
                },
                keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to insert tracked_link and retrieve generated id");
        }

        return new TrackedLink(
                key.longValue(), link.url(), link.type(), link.createdAt(), link.lastCheckedAt(), link.lastUpdatedAt());
    }

    @Override
    public Optional<TrackedLink> findById(long id) {
        return jdbcClient
                .sql("select * from tracked_link where id = :id")
                .param("id", id)
                .query(LINK_ROW_MAPPER)
                .optional();
    }

    @Override
    public List<TrackedLink> findAllById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jdbcClient
                .sql("select * from tracked_link where id in (:ids) order by id")
                .param("ids", ids)
                .query(LINK_ROW_MAPPER)
                .list();
    }

    @Override
    public Optional<TrackedLink> findByUrl(String url) {
        return jdbcClient
                .sql("select * from tracked_link where url = :url")
                .param("url", url)
                .query(LINK_ROW_MAPPER)
                .optional();
    }

    @Override
    public List<TrackedLink> findAll() {
        return jdbcClient
                .sql("select * from tracked_link order by id")
                .query(LINK_ROW_MAPPER)
                .list();
    }

    @Override
    public List<TrackedLink> findPage(long offset, int limit) {
        return jdbcClient
                .sql("select * from tracked_link order by id limit :limit offset :offset")
                .param("limit", limit)
                .param("offset", offset)
                .query(LINK_ROW_MAPPER)
                .list();
    }

    @Override
    public void updatePollingState(long linkId, Instant lastCheckedAt, Instant lastUpdatedAt) {
        jdbcClient
                .sql("""
                update tracked_link
                set last_checked_at = :lastCheckedAt,
                    last_updated_at = :lastUpdatedAt
                where id = :id
                """)
                .param("lastCheckedAt", toOffsetDateTime(lastCheckedAt))
                .param("lastUpdatedAt", toOffsetDateTime(lastUpdatedAt))
                .param("id", linkId)
                .update();
    }

    @Override
    public void deleteById(long id) {
        jdbcClient
                .sql("delete from tracked_link where id = :id")
                .param("id", id)
                .update();
    }

    private static TrackedLink mapRow(ResultSet rs, int ignoredRowNum) throws SQLException {
        return new TrackedLink(
                rs.getLong("id"),
                rs.getString("url"),
                LinkSourceType.valueOf(rs.getString("type")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_checked_at") != null
                        ? rs.getTimestamp("last_checked_at").toInstant()
                        : null,
                rs.getTimestamp("last_updated_at") != null
                        ? rs.getTimestamp("last_updated_at").toInstant()
                        : null);
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
