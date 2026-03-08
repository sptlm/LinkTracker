package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.api.exception.ChatAlreadyRegisteredException;
import backend.academy.linktracker.scrapper.api.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final SubscriptionRepository subscriptionRepository;

    public void register(long chatId) {
        if (chatRepository.exists(chatId)) {
            throw new ChatAlreadyRegisteredException(chatId);
        }

        chatRepository.save(chatId);
    }

    public void delete(long chatId) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }

        subscriptionRepository
                .findByChatId(chatId)
                .forEach(subscription -> subscriptionRepository.delete(subscription.chatId(), subscription.linkId()));

        chatRepository.delete(chatId);
    }

    public void ensureExists(long chatId) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
    }
}
