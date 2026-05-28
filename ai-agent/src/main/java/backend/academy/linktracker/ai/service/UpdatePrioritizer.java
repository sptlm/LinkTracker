package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdatePrioritizer {

    private final AiAgentProperties properties;

    public UpdatePriority prioritize(String description) {
        String normalizedDescription = normalize(description);
        if (containsAny(normalizedDescription, properties.getPrioritization().getHighKeywords())) {
            return UpdatePriority.HIGH;
        }
        if (containsAny(normalizedDescription, properties.getPrioritization().getLowKeywords())) {
            return UpdatePriority.LOW;
        }
        return UpdatePriority.MEDIUM;
    }

    private boolean containsAny(String description, Iterable<String> keywords) {
        for (String keyword : keywords) {
            String normalizedKeyword = normalize(keyword);
            if (!normalizedKeyword.isBlank() && description.contains(normalizedKeyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().trim();
    }
}
