package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.model.LinkSubscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemorySubscriptionRepository implements SubscriptionRepository {

    private final Map<String, LinkSubscription> subscriptions = new ConcurrentHashMap<>();

    @Override
    public boolean exists(long chatId, long linkId) {
        return subscriptions.containsKey(key(chatId, linkId));
    }

    @Override
    public LinkSubscription save(LinkSubscription subscription) {
        subscriptions.put(key(subscription.chatId(), subscription.linkId()), subscription);
        return subscription;
    }

    @Override
    public Optional<LinkSubscription> findByChatIdAndLinkId(long chatId, long linkId) {
        return Optional.ofNullable(subscriptions.get(key(chatId, linkId)));
    }

    @Override
    public List<LinkSubscription> findByChatId(long chatId) {
        return subscriptions.values().stream()
            .filter(subscription -> subscription.chatId() == chatId)
            .toList();
    }

    @Override
    public List<LinkSubscription> findByLinkId(long linkId) {
        return subscriptions.values().stream()
            .filter(subscription -> subscription.linkId() == linkId)
            .toList();
    }

    @Override
    public List<Long> findChatIdsByLinkId(long linkId) {
        return subscriptions.values().stream()
            .filter(subscription -> subscription.linkId() == linkId)
            .map(LinkSubscription::chatId)
            .distinct()
            .toList();
    }

    @Override
    public void delete(long chatId, long linkId) {
        subscriptions.remove(key(chatId, linkId));
    }

    @Override
    public boolean hasSubscribers(long linkId) {
        return subscriptions.values().stream()
            .anyMatch(subscription -> subscription.linkId() == linkId);
    }

    private String key(long chatId, long linkId) {
        return chatId + ":" + linkId;
    }
}
