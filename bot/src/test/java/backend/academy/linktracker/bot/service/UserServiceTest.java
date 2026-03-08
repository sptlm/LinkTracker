package backend.academy.linktracker.bot.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.model.User;
import backend.academy.linktracker.bot.repository.UserRepository;
import com.pengrad.telegrambot.model.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void isRegistered_returnsRepositoryValue() {
        when(userRepository.existsByChatId(123L)).thenReturn(true);

        assertTrue(userService.isRegistered(123L));
    }

    @Test
    void registerIfAbsent_whenUserAlreadyExists_returnsExistingUser() {
        Message message = org.mockito.Mockito.mock(Message.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        User existingUser = User.builder()
                .chatId(123L)
                .username("spirit")
                .firstName("Spirit")
                .lastName("User")
                .build();

        when(message.chat().id()).thenReturn(123L);
        when(userRepository.findByChatId(123L)).thenReturn(java.util.Optional.of(existingUser));

        RegistrationResult result = userService.registerIfAbsent(message);

        assertSame(existingUser, result.user());
        assertFalse(result.created());
        verify(userRepository, never()).save(org.mockito.Mockito.any());
    }

    @Test
    void registerIfAbsent_whenUserDoesNotExist_savesNewUser() {
        Message message = org.mockito.Mockito.mock(Message.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);

        when(message.chat().id()).thenReturn(123L);
        when(message.from().username()).thenReturn("spirit");
        when(message.from().firstName()).thenReturn("Spirit");
        when(message.from().lastName()).thenReturn("User");
        when(userRepository.findByChatId(123L)).thenReturn(java.util.Optional.empty());

        RegistrationResult result = userService.registerIfAbsent(message);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(123L, savedUser.getChatId());
        org.junit.jupiter.api.Assertions.assertEquals("spirit", savedUser.getUsername());
        org.junit.jupiter.api.Assertions.assertEquals("Spirit", savedUser.getFirstName());
        org.junit.jupiter.api.Assertions.assertEquals("User", savedUser.getLastName());
        assertNotNull(savedUser.getRegisteredAt());

        assertTrue(result.created());
        assertSame(savedUser, result.user());
    }
}
