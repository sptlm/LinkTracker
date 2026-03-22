package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.LinkSubscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.LinkSubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.LinkSubscriptionId;
import backend.academy.linktracker.scrapper.repository.orm.jpa.LinkSubscriptionJpaRepository;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "access-type", havingValue = "ORM")
public class OrmSubscriptionRepository implements SubscriptionRepository {

    private final LinkSubscriptionJpaRepository linkSubscriptionJpaRepository;

    public OrmSubscriptionRepository(LinkSubscriptionJpaRepository linkSubscriptionJpaRepository) {
        this.linkSubscriptionJpaRepository = linkSubscriptionJpaRepository;
    }

    @Override
    public boolean exists(long chatId, long linkId) {
        return linkSubscriptionJpaRepository.existsByIdChatIdAndIdLinkId(chatId, linkId);
    }

    @Override
    public LinkSubscription save(LinkSubscription subscription) {
        linkSubscriptionJpaRepository.save(toEntity(subscription));
        return subscription;
    }

    @Override
    public java.util.Optional<LinkSubscription> findByChatIdAndLinkId(long chatId, long linkId) {
        return linkSubscriptionJpaRepository
                .findById(new LinkSubscriptionId(chatId, linkId))
                .map(this::toModel);
    }

    @Override
    public List<LinkSubscription> findByChatId(long chatId) {
        return linkSubscriptionJpaRepository.findAllByIdChatIdOrderByIdLinkIdAsc(chatId).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<LinkSubscription> findByLinkId(long linkId) {
        return linkSubscriptionJpaRepository.findAllByIdLinkIdOrderByIdChatIdAsc(linkId).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<Long> findChatIdsByLinkId(long linkId) {
        return linkSubscriptionJpaRepository.findChatIdsByLinkId(linkId);
    }

    @Override
    public void delete(long chatId, long linkId) {
        linkSubscriptionJpaRepository.deleteById(new LinkSubscriptionId(chatId, linkId));
    }

    @Override
    public boolean hasSubscribers(long linkId) {
        return linkSubscriptionJpaRepository.countByIdLinkId(linkId) > 0;
    }

    private LinkSubscriptionEntity toEntity(LinkSubscription subscription) {
        return new LinkSubscriptionEntity(
                new LinkSubscriptionId(subscription.chatId(), subscription.linkId()),
                subscription.createdAt(),
                new LinkedHashSet<>(subscription.tags()));
    }

    private LinkSubscription toModel(LinkSubscriptionEntity entity) {
        return new LinkSubscription(
                entity.getId().getChatId(),
                entity.getId().getLinkId(),
                entity.getTags().stream().sorted().toList(),
                entity.getCreatedAt());
    }
}
