package backend.academy.linktracker.scrapper.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.api.exception.ChatAlreadyRegisteredException;
import backend.academy.linktracker.scrapper.api.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.model.LinkSubscription;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private LinkListCacheService linkListCacheService;

    @InjectMocks
    private ChatService chatService;

    @Test
    void register_whenChatAlreadyExists_throwsException() {
        when(chatRepository.exists(123L)).thenReturn(true);

        assertThrows(ChatAlreadyRegisteredException.class, () -> chatService.register(123L));
    }

    @Test
    void register_whenChatDoesNotExist_savesChat() {
        when(chatRepository.exists(123L)).thenReturn(false);

        chatService.register(123L);

        verify(chatRepository).save(123L);
    }

    @Test
    void delete_whenChatDoesNotExist_throwsException() {
        when(chatRepository.exists(123L)).thenReturn(false);

        assertThrows(ChatNotFoundException.class, () -> chatService.delete(123L));
    }

    @Test
    void delete_whenChatExists_deletesSubscriptionsAndOrphanLinks() {
        LinkSubscription first = new LinkSubscription(123L, 10L, List.of("java"), Instant.now());
        LinkSubscription second = new LinkSubscription(123L, 20L, List.of("sql"), Instant.now());

        when(chatRepository.exists(123L)).thenReturn(true);
        when(subscriptionRepository.findByChatId(123L)).thenReturn(List.of(first, second));
        when(subscriptionRepository.hasSubscribers(10L)).thenReturn(false);
        when(subscriptionRepository.hasSubscribers(20L)).thenReturn(true);

        chatService.delete(123L);

        verify(subscriptionRepository).delete(123L, 10L);
        verify(subscriptionRepository).delete(123L, 20L);
        verify(linkRepository).deleteById(10L);
        verify(linkRepository, never()).deleteById(20L);
        verify(chatRepository).delete(123L);
    }

    @Test
    void ensureExists_whenChatExists_doesNothing() {
        when(chatRepository.exists(123L)).thenReturn(true);

        assertDoesNotThrow(() -> chatService.ensureExists(123L));
    }

    @Test
    void ensureExists_whenChatMissing_throwsException() {
        when(chatRepository.exists(123L)).thenReturn(false);

        assertThrows(ChatNotFoundException.class, () -> chatService.ensureExists(123L));
    }
}
