package backend.academy.linktracker.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.properties.BotMessagesProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BotMessagesServiceTest {

    @Mock
    private BotMessagesProperties properties;

    @InjectMocks
    private BotMessagesService botMessagesService;

    @Test
    void startWelcome_formatsDisplayName() {
        when(properties.getStartWelcome()).thenReturn("Привет, %s!");

        String result = botMessagesService.startWelcome("Spirit");

        assertEquals("Привет, Spirit!", result);
    }

    @Test
    void welcomeBack_formatsDisplayName() {
        when(properties.getWelcomeBack()).thenReturn("С возвращением, %s!");

        String result = botMessagesService.welcomeBack("Spirit");

        assertEquals("С возвращением, Spirit!", result);
    }

    @Test
    void updatesMessage_usesFallbackDescription_whenDescriptionIsBlank() {
        when(properties.getUpdatesTemplate()).thenReturn("Обновление по %s: %s");

        String result = botMessagesService.updatesMessage("https://github.com/user/repo", " ");

        assertEquals("Обновление по https://github.com/user/repo: Есть новые изменения.", result);
    }

    @Test
    void updatesMessage_usesProvidedDescription_whenDescriptionIsPresent() {
        when(properties.getUpdatesTemplate()).thenReturn("Обновление по %s: %s");

        String result = botMessagesService.updatesMessage("https://github.com/user/repo", "Новый коммит в main");

        assertEquals("Обновление по https://github.com/user/repo: Новый коммит в main", result);
    }
}
