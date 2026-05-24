package backend.academy.linktracker.ai.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import org.junit.jupiter.api.Test;

class AiAgentPropertiesValidatorTest {

    @Test
    void shouldFailWhenApiProviderHasNoToken() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getSummarization().setProvider(AiAgentProperties.Provider.API);
        properties.getSummarization().getApi().setBaseUrl("https://generativelanguage.googleapis.com/v1beta");
        properties.getSummarization().getApi().setModel("gemini-3.5-flash");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> new AiAgentPropertiesValidator(properties).afterPropertiesSet());

        assertTrue(exception.getMessage().contains("ai-agent.summarization.api.token"));
    }

    @Test
    void shouldPassWhenApiProviderIsFullyConfigured() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getSummarization().setProvider(AiAgentProperties.Provider.API);
        properties.getSummarization().getApi().setBaseUrl("https://generativelanguage.googleapis.com/v1beta");
        properties.getSummarization().getApi().setToken("token");
        properties.getSummarization().getApi().setModel("gemini-3.5-flash");

        assertDoesNotThrow(() -> new AiAgentPropertiesValidator(properties).afterPropertiesSet());
    }

    @Test
    void shouldPassWhenStubProviderHasNoApiConfiguration() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getSummarization().setProvider(AiAgentProperties.Provider.STUB);

        assertDoesNotThrow(() -> new AiAgentPropertiesValidator(properties).afterPropertiesSet());
    }
}
