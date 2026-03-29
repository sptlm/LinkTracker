package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.LinkSubscription;
import backend.academy.linktracker.scrapper.repository.orm.entity.LinkSubscriptionEntity;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Component;

@Component
public class LinkSubscriptionOrmConverter {

    public LinkSubscriptionEntity toEntity(LinkSubscription subscription) {
        return new LinkSubscriptionEntity(
                null,
                subscription.chatId(),
                subscription.linkId(),
                subscription.createdAt(),
                new LinkedHashSet<>(subscription.tags()));
    }

    public LinkSubscription toModel(LinkSubscriptionEntity entity) {
        return new LinkSubscription(
                entity.getChatId(),
                entity.getLinkId(),
                entity.getTags().stream().sorted().toList(),
                entity.getCreatedAt());
    }
}
