package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.LinkSubscription;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {

    boolean exists(long chatId, long linkId);

    LinkSubscription save(LinkSubscription subscription);

    Optional<LinkSubscription> findByChatIdAndLinkId(long chatId, long linkId);

    List<LinkSubscription> findByChatId(long chatId);

    List<LinkSubscription> findByLinkId(long linkId);

    List<Long> findChatIdsByLinkId(long linkId);

    void delete(long chatId, long linkId);

    boolean hasSubscribers(long linkId);
}
