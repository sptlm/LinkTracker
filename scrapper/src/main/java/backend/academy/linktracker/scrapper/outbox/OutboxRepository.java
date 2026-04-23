package backend.academy.linktracker.scrapper.outbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    private static final RowMapper<OutboxEvent> ROW_MAPPER = new RowMapper<>() {
        @Override
        public OutboxEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new OutboxEvent(
                    rs.getLong("id"),
                    rs.getString("payload"),
                    rs.getInt("attempts"),
                    rs.getObject("created_at", OffsetDateTime.class));
        }
    };

    private final JdbcTemplate jdbcTemplate;

    public void enqueue(String payload) {
        jdbcTemplate.update(
                """
                INSERT INTO notification_outbox(payload, status, attempts)
                VALUES (?, 'PENDING', 0)
                """,
                payload);
    }

    public List<OutboxEvent> findPendingBatch(int limit, int maxAttempts) {
        return jdbcTemplate.query(
                """
                SELECT id, payload, attempts, created_at
                FROM notification_outbox
                WHERE status = 'PENDING'
                  AND attempts < ?
                ORDER BY created_at ASC
                LIMIT ?
                """,
                ROW_MAPPER,
                maxAttempts,
                limit);
    }

    public void markSent(long id) {
        jdbcTemplate.update(
                """
                UPDATE notification_outbox
                SET status = 'SENT', sent_at = NOW()
                WHERE id = ?
                """,
                id);
    }

    public void incrementAttempts(long id) {
        jdbcTemplate.update(
                """
                UPDATE notification_outbox
                SET attempts = attempts + 1
                WHERE id = ?
                """,
                id);
    }

    public void markFailed(long id) {
        jdbcTemplate.update(
                """
                UPDATE notification_outbox
                SET status = 'FAILED'
                WHERE id = ?
                """,
                id);
    }
}
