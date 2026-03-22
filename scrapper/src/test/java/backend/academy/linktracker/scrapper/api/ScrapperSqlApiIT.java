package backend.academy.linktracker.scrapper.api;

import backend.academy.linktracker.scrapper.repository.sql.SqlChatRepository;
import backend.academy.linktracker.scrapper.repository.sql.SqlLinkRepository;
import backend.academy.linktracker.scrapper.repository.sql.SqlSubscriptionRepository;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.persistence.access-type=SQL")
class ScrapperSqlApiIT extends AbstractScrapperApiIT {

    @Override
    protected Class<?> expectedLinkRepositoryType() {
        return SqlLinkRepository.class;
    }

    @Override
    protected Class<?> expectedChatRepositoryType() {
        return SqlChatRepository.class;
    }

    @Override
    protected Class<?> expectedSubscriptionRepositoryType() {
        return SqlSubscriptionRepository.class;
    }
}
