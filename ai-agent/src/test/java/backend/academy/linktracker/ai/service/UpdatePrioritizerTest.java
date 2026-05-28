package backend.academy.linktracker.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdatePrioritizerTest {

    private UpdatePrioritizer prioritizer;

    @BeforeEach
    void setUp() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getPrioritization().setHighKeywords(List.of("critical", "urgent", "security"));
        properties.getPrioritization().setLowKeywords(List.of("minor", "typo", "docs"));
        prioritizer = new UpdatePrioritizer(properties);
    }

    @Test
    void shouldReturnHighWhenTextContainsHighKeyword() {
        assertEquals(UpdatePriority.HIGH, prioritizer.prioritize("critical bug fix"));
    }

    @Test
    void shouldReturnMediumWhenTextContainsNoKeywords() {
        assertEquals(UpdatePriority.MEDIUM, prioritizer.prioritize("regular repository update"));
    }

    @Test
    void shouldReturnLowWhenTextContainsLowKeyword() {
        assertEquals(UpdatePriority.LOW, prioritizer.prioritize("fix typo in readme"));
    }

    @Test
    void shouldPreferHighWhenTextContainsHighAndLowKeywords() {
        assertEquals(UpdatePriority.HIGH, prioritizer.prioritize("urgent docs update"));
    }
}
