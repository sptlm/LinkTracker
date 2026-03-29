package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.repository.TagRepository;
import backend.academy.linktracker.scrapper.repository.orm.jpa.LinkSubscriptionJpaRepository;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "access-type", havingValue = "ORM")
public class OrmTagRepository implements TagRepository {

    private final LinkSubscriptionJpaRepository linkSubscriptionJpaRepository;

    public OrmTagRepository(LinkSubscriptionJpaRepository linkSubscriptionJpaRepository) {
        this.linkSubscriptionJpaRepository = linkSubscriptionJpaRepository;
    }

    @Override
    public boolean exists(long chatId, long linkId, String tag) {
        return linkSubscriptionJpaRepository
                .findByChatIdAndLinkId(chatId, linkId)
                .map(subscription -> subscription.getTags().contains(tag))
                .orElse(false);
    }

    @Override
    public List<String> findByChatIdAndLinkId(long chatId, long linkId) {
        return linkSubscriptionJpaRepository
                .findByChatIdAndLinkId(chatId, linkId)
                .map(subscription -> subscription.getTags().stream().sorted().toList())
                .orElse(List.of());
    }

    @Override
    @Transactional
    public void add(long chatId, long linkId, String tag) {
        linkSubscriptionJpaRepository.findByChatIdAndLinkId(chatId, linkId).ifPresent(subscription -> {
            subscription.getTags().add(tag);
            linkSubscriptionJpaRepository.save(subscription);
        });
    }

    @Override
    @Transactional
    public void replace(long chatId, long linkId, List<String> tags) {
        linkSubscriptionJpaRepository.findByChatIdAndLinkId(chatId, linkId).ifPresent(subscription -> {
            subscription.setTags(new LinkedHashSet<>(tags));
            linkSubscriptionJpaRepository.save(subscription);
        });
    }

    @Override
    @Transactional
    public void delete(long chatId, long linkId, String tag) {
        linkSubscriptionJpaRepository.findByChatIdAndLinkId(chatId, linkId).ifPresent(subscription -> {
            subscription.getTags().remove(tag);
            linkSubscriptionJpaRepository.save(subscription);
        });
    }
}
