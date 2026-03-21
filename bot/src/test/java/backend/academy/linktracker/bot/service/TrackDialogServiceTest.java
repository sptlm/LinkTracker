package backend.academy.linktracker.bot.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.scrapper.ChatNotRegisteredException;
import backend.academy.linktracker.bot.client.scrapper.DuplicateLinkException;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClientException;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.model.DialogSessionKey;
import backend.academy.linktracker.bot.repository.UserDialogStateRepository;
import backend.academy.linktracker.bot.state.TrackDialogState;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackDialogServiceTest {

    @Mock
    private UserDialogStateRepository stateRepository;

    @Mock
    private SupportedLinkParser supportedLinkParser;

    @Mock
    private LinkTrackingService linkTrackingService;

    @Mock
    private BotMessagesService messages;

    @Mock
    private CommandContext context;

    @InjectMocks
    private TrackDialogService trackDialogService;

    private final long chatId = 123L;
    private final long userId = 456L;
    private DialogSessionKey sessionKey;

    @BeforeEach
    void setUp() {
        when(context.chatId()).thenReturn(chatId);
        when(context.userId()).thenReturn(userId);
        sessionKey = new DialogSessionKey(chatId, userId);
    }

    @Test
    void processIfActive_returnsFalse_whenNoDialogExists() {
        when(stateRepository.findById(sessionKey)).thenReturn(Optional.empty());

        boolean processed = trackDialogService.processIfActive(context);

        assertFalse(processed);
        verify(context, never()).reply(any());
    }


    /**
     * Требование: Пользователь отправляет /track и корректную ссылку (например, https://github.com/user/repo),
     * а затем теги и фильтры.
     * Данные сохранены в локальное хранилище.
     */
    @Test
    void processIfActive_whenWaitingLinkAndValidLink_savesWaitingTagsState() {
        URI uri = URI.create("https://github.com/user/repo");

        when(stateRepository.findById(sessionKey)).thenReturn(Optional.of(TrackDialogState.waitingLink()));
        when(context.messageText()).thenReturn("https://github.com/user/repo");
        when(supportedLinkParser.parse("https://github.com/user/repo")).thenReturn(Optional.of(uri));
        when(messages.trackAskTags()).thenReturn("Теперь пришлите теги");

        boolean processed = trackDialogService.processIfActive(context);

        assertTrue(processed);
        verify(stateRepository).save(sessionKey, TrackDialogState.waitingTags(uri));
        verify(context).reply("Теперь пришлите теги");
    }

    /**
     * Требование: Пользователь отправляет /track и корректную ссылку (например, https://github.com/user/repo),
     * а затем теги и фильтры.
     * Данные сохранены в локальное хранилище.
     */
    @Test
    void processIfActive_whenWaitingTags_savesWaitingFiltersState() {
        URI uri = URI.create("https://github.com/user/repo");
        TrackDialogState state = TrackDialogState.waitingTags(uri);

        when(stateRepository.findById(sessionKey)).thenReturn(Optional.of(state));
        when(context.messageText()).thenReturn("java, backend");
        when(messages.trackAskFilters()).thenReturn("Теперь пришлите фильтры");

        boolean processed = trackDialogService.processIfActive(context);

        assertTrue(processed);
        verify(stateRepository).save(sessionKey, TrackDialogState.waitingFilters(uri, List.of("java", "backend")));
        verify(context).reply("Теперь пришлите фильтры");
    }

    /**
     * Требование: Пользователь отправляет /track и некорректную ссылку (например, tbank://github.com/user/repo).
     * Бот уведомляет пользователя, что ссылка некорректна.
     */
    @Test
    void processIfActive_whenWaitingLinkAndInvalidLink_repliesInvalidLink() {
        when(stateRepository.findById(sessionKey)).thenReturn(Optional.of(TrackDialogState.waitingLink()));
        when(context.messageText()).thenReturn("tbank://github.com/user/repo");
        when(supportedLinkParser.parse("tbank://github.com/user/repo")).thenReturn(Optional.empty());
        when(messages.invalidLink()).thenReturn("Некорректная ссылка");

        boolean processed = trackDialogService.processIfActive(context);

        assertTrue(processed);
        verify(context).reply("Некорректная ссылка");
        verify(stateRepository, never()).save(sessionKey, TrackDialogState.waitingLink());
        verify(linkTrackingService, never()).addLink(any(Long.class), any(), any(), any());
    }

    @Test
    void processIfActive_whenWaitingFiltersAndDash_savesWithEmptyFilters() {
        URI uri = URI.create("https://github.com/user/repo");
        TrackDialogState state = TrackDialogState.waitingFilters(uri, List.of("java", "spring"));

        when(stateRepository.findById(sessionKey)).thenReturn(Optional.of(state));
        when(context.messageText()).thenReturn("-");
        when(messages.linkTracked()).thenReturn("Ссылка добавлена");

        boolean processed = trackDialogService.processIfActive(context);

        assertTrue(processed);

        verify(linkTrackingService)
                .addLink(chatId, "https://github.com/user/repo", List.of("java", "spring"), List.of());
        verify(stateRepository).delete(sessionKey);
        verify(context).reply("Ссылка добавлена");
    }

    /**
     * Требование: Пользователь отправляет /track и корректную ссылку (например, https://github.com/user/repo),
     * а затем теги и фильтры.
     * Данные сохранены в локальное хранилище.
     */
    @Test
    void processIfActive_whenWaitingFiltersAndSuccess_repliesLinkTracked() {
        URI uri = URI.create("https://github.com/user/repo");
        TrackDialogState state = TrackDialogState.waitingFilters(uri, List.of("java"));

        when(stateRepository.findById(sessionKey)).thenReturn(Optional.of(state));
        when(context.messageText()).thenReturn("branch=main, author=spirit");
        when(messages.linkTracked()).thenReturn("Ссылка добавлена");

        boolean processed = trackDialogService.processIfActive(context);

        assertTrue(processed);

        verify(linkTrackingService)
                .addLink(
                        chatId,
                        "https://github.com/user/repo",
                        List.of("java"),
                        List.of("branch=main", "author=spirit"));
        verify(stateRepository).delete(sessionKey);
        verify(context).reply("Ссылка добавлена");
    }

    /**
     * Требование: Пользователь в рамках запроса /track отправляет ссылку, на которую уже подписан.
     * Бот уведомляет пользователя, что он уже подписан на эту ссылку.
     */
    @Test
    void processIfActive_whenDuplicateLink_repliesAlreadyTracked() {
        URI uri = URI.create("https://github.com/user/repo");
        TrackDialogState state = TrackDialogState.waitingFilters(uri, List.of("java"));

        when(stateRepository.findById(sessionKey)).thenReturn(Optional.of(state));
        when(context.messageText()).thenReturn("branch=main");
        when(messages.alreadyTracked()).thenReturn("Вы уже подписаны на эту ссылку");
        org.mockito.Mockito.doThrow(new DuplicateLinkException("duplicate"))
                .when(linkTrackingService)
                .addLink(chatId, "https://github.com/user/repo", List.of("java"), List.of("branch=main"));

        boolean processed = trackDialogService.processIfActive(context);

        assertTrue(processed);
        verify(stateRepository).delete(sessionKey);
        verify(context).reply("Вы уже подписаны на эту ссылку");
    }

    @Test
    void processIfActive_whenChatNotRegistered_repliesChatNotRegistered() {
        URI uri = URI.create("https://github.com/user/repo");
        TrackDialogState state = TrackDialogState.waitingFilters(uri, List.of("java"));

        when(stateRepository.findById(sessionKey)).thenReturn(Optional.of(state));
        when(context.messageText()).thenReturn("branch=main");
        when(messages.chatNotRegistered()).thenReturn("Чат не зарегистрирован");
        org.mockito.Mockito.doThrow(new ChatNotRegisteredException("not registered"))
                .when(linkTrackingService)
                .addLink(chatId, "https://github.com/user/repo", List.of("java"), List.of("branch=main"));

        boolean processed = trackDialogService.processIfActive(context);

        assertTrue(processed);
        verify(stateRepository).delete(sessionKey);
        verify(context).reply("Чат не зарегистрирован");
    }

    @Test
    void processIfActive_whenScrapperUnavailable_repliesScrapperUnavailable() {
        URI uri = URI.create("https://github.com/user/repo");
        TrackDialogState state = TrackDialogState.waitingFilters(uri, List.of("java"));

        when(stateRepository.findById(sessionKey)).thenReturn(Optional.of(state));
        when(context.messageText()).thenReturn("branch=main");
        when(messages.scrapperUnavailable()).thenReturn("Scrapper временно недоступен");
        org.mockito.Mockito.doThrow(new ScrapperClientException("unavailable"))
                .when(linkTrackingService)
                .addLink(chatId, "https://github.com/user/repo", List.of("java"), List.of("branch=main"));

        boolean processed = trackDialogService.processIfActive(context);

        assertTrue(processed);
        verify(stateRepository).delete(sessionKey);
        verify(context).reply("Scrapper временно недоступен");
    }
}
