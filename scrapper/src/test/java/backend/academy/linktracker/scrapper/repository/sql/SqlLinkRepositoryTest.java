package backend.academy.linktracker.scrapper.repository.sql;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.KeyHolder;

@ExtendWith(MockitoExtension.class)
class SqlLinkRepositoryTest {

    @Mock
    private JdbcClient jdbcClient;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void save_allowsNullPollingFieldsWithoutNpeDuringArgumentPreparation() {
        SqlLinkRepository repository = new SqlLinkRepository(jdbcClient, jdbcTemplate);

        TrackedLink link =
                new TrackedLink(0L, "https://github.com/user/repo", LinkSourceType.GITHUB, Instant.now(), null, null);

        doThrow(new RuntimeException("jdbc-called"))
                .when(jdbcTemplate)
                .update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> repository.save(link));
        org.junit.jupiter.api.Assertions.assertEquals("jdbc-called", ex.getMessage());
    }
}
