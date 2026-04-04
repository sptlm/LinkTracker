package backend.academy.linktracker.scrapper.api;

import backend.academy.linktracker.scrapper.repository.orm.OrmChatRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmLinkRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmSubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmTagRepository;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.persistence.access-type=ORM")
class ScrapperOrmApiIT extends AbstractScrapperApiIT {

    @Override
    protected Class<?> expectedLinkRepositoryType() {
        return OrmLinkRepository.class;
    }

    @Override
    protected Class<?> expectedChatRepositoryType() {
        return OrmChatRepository.class;
    }

    @Override
    protected Class<?> expectedSubscriptionRepositoryType() {
        return OrmSubscriptionRepository.class;
    }

    @Override
    protected Class<?> expectedTagRepositoryType() {
        return OrmTagRepository.class;
    }

    @Override
    protected boolean expectEntityManagerFactory() {
        return true;
    }
}
