package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.LinkSubscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.orm.jpa.LinkSubscriptionJpaRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "access-type", havingValue = "ORM")
public class OrmSubscriptionRepository implements SubscriptionRepository {

    private final LinkSubscriptionJpaRepository linkSubscriptionJpaRepository;
    private final LinkSubscriptionOrmConverter converter;

    public OrmSubscriptionRepository(
        LinkSubscriptionJpaRepository linkSubscriptionJpaRepository, LinkSubscriptionOrmConverter converter) {
        this.linkSubscriptionJpaRepository = linkSubscriptionJpaRepository;
        this.converter = converter;
    }

    @Override
    public boolean exists(long chatId, long linkId) {
        return linkSubscriptionJpaRepository.existsByChatIdAndLinkId(chatId, linkId);
    }

    @Override
    public LinkSubscription save(LinkSubscription subscription) {
        linkSubscriptionJpaRepository.save(converter.toEntity(subscription));
        return subscription;
    }

    @Override
    public java.util.Optional<LinkSubscription> findByChatIdAndLinkId(long chatId, long linkId) {
        return linkSubscriptionJpaRepository.findByChatIdAndLinkId(chatId, linkId).map(converter::toModel);
    }

    @Override
    public List<LinkSubscription> findByChatId(long chatId) {
        return linkSubscriptionJpaRepository.findAllByChatIdOrderByLinkIdAsc(chatId).stream()
            .map(converter::toModel)
            .toList();
    }

    @Override
    public List<LinkSubscription> findByLinkId(long linkId) {
        return linkSubscriptionJpaRepository.findAllByLinkIdOrderByChatIdAsc(linkId).stream()
            .map(converter::toModel)
            .toList();
    }

    @Override
    public List<Long> findChatIdsByLinkId(long linkId) {
        return linkSubscriptionJpaRepository.findChatIdsByLinkId(linkId);
    }

    @Override
    public void delete(long chatId, long linkId) {
        linkSubscriptionJpaRepository.deleteByChatIdAndLinkId(chatId, linkId);
    }

    @Override
    public boolean hasSubscribers(long linkId) {
        return linkSubscriptionJpaRepository.countByLinkId(linkId) > 0;
    }
}
