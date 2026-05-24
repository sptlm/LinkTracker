package backend.academy.linktracker.ai.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateFilterTest {

    private AiAgentProperties properties;
    private UpdateFilter filter;

    @BeforeEach
    void setUp() {
        properties = new AiAgentProperties();
        properties.getFiltering().setStopWords(List.of("spam", "promo"));
        properties.getFiltering().setExcludedAuthors(List.of("bot-user"));
        properties.getFiltering().setMinLength(20);
        filter = new UpdateFilter(properties);
    }

    @Test
    void shouldFilterByStopWord() {
        LinkUpdate update = update("This update contains spam content", "alice");

        assertFalse(filter.shouldProcess(update));
    }

    @Test
    void shouldFilterByExcludedAuthor() {
        LinkUpdate update = update("This update has enough useful content", " bot-user ");

        assertFalse(filter.shouldProcess(update));
    }

    @Test
    void shouldFilterByMinimumLength() {
        LinkUpdate update = update("                    ", "alice");

        assertFalse(filter.shouldProcess(update));
    }

    @Test
    void shouldPassValidUpdate() {
        LinkUpdate update = update("This update has enough useful content", "alice");

        assertTrue(filter.shouldProcess(update));
    }

    @Test
    void shouldNotFilterStopWordAsPartOfAnotherWord() {
        properties.getFiltering().setStopWords(List.of("ads"));
        LinkUpdate update = update("This update discusses shadows in enough detail", "alice");

        assertTrue(filter.shouldProcess(update));
    }

    private LinkUpdate update(String description, String author) {
        return new LinkUpdate()
                .id(1L)
                .url(URI.create("https://example.com"))
                .description(description)
                .author(author)
                .tgChatIds(List.of(1L));
    }
}
