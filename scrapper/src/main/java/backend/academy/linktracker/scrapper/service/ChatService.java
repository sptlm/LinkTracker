package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.api.exception.ChatAlreadyRegisteredException;
import backend.academy.linktracker.scrapper.api.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LinkRepository linkRepository;

    @Transactional
    public void register(long chatId) {
        if (chatRepository.exists(chatId)) {
            throw new ChatAlreadyRegisteredException(chatId);
        }

        chatRepository.save(chatId);
    }

    @Transactional
    public void delete(long chatId) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }

        subscriptionRepository.findByChatId(chatId).forEach(subscription -> {
            subscriptionRepository.delete(chatId, subscription.linkId());
            if (!subscriptionRepository.hasSubscribers(subscription.linkId())) {
                linkRepository.deleteById(subscription.linkId());
            }
        });

        chatRepository.delete(chatId);
    }

    public void ensureExists(long chatId) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
    }
}
