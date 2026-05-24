package backend.academy.linktracker.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiAgentUpdateProcessorTest {

    private AiAgentUpdateProcessor processor;

    @BeforeEach
    void setUp() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getFiltering().setMinLength(0);
        properties.getSummarization().setProvider(AiAgentProperties.Provider.API);
        properties.getSummarization().setThreshold(10);

        TruncatingSummarizer truncatingSummarizer = new TruncatingSummarizer();
        AiApiSummarizer aiApiSummarizer =
                new AiApiSummarizer(properties, JsonMapper.builder().build());
        FallbackSummarizer fallbackSummarizer =
                new FallbackSummarizer(properties, aiApiSummarizer, truncatingSummarizer);
        processor = new AiAgentUpdateProcessor(properties, new UpdateFilter(properties), fallbackSummarizer);
    }

    @Test
    void shouldSummarizeLongTextWithFallbackWhenApiIsNotConfigured() {
        LinkUpdate update = update("123456789012345");

        LinkUpdate processed = processor.process(update).orElseThrow();

        assertEquals("1234567...", processed.getDescription());
        assertEquals("HIGH", processed.getPriority());
    }

    @Test
    void shouldPassShortTextWithoutSummarization() {
        LinkUpdate update = update("short");

        LinkUpdate processed = processor.process(update).orElseThrow();

        assertEquals("short", processed.getDescription());
        assertSame(processed.getDescription(), update.getDescription());
    }

    private LinkUpdate update(String description) {
        return new LinkUpdate()
                .id(1L)
                .url(URI.create("https://example.com"))
                .description(description)
                .author("alice")
                .tgChatIds(List.of(1L));
    }
}
