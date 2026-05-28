package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateFilter {

    private final AiAgentProperties properties;

    public boolean shouldProcess(LinkUpdate update) {
        if (update == null) {
            log.debug("Update filtered out because payload is null");
            return false;
        }
        if (update.getDescription() == null) {
            logFiltered(update, "description is null");
            return false;
        }

        AiAgentProperties.Filtering filtering = properties.getFiltering();
        String description = update.getDescription();
        if (!hasEnoughText(description, filtering)) {
            logFiltered(update, "description is shorter than configured minimum length");
            return false;
        }
        if (!hasNoStopWords(description, filtering)) {
            logFiltered(update, "description contains a stop word");
            return false;
        }
        if (!hasAllowedAuthor(update.getAuthor(), filtering)) {
            logFiltered(update, "author is excluded");
            return false;
        }
        return true;
    }

    private boolean hasEnoughText(String description, AiAgentProperties.Filtering filtering) {
        return description.trim().length() >= filtering.getMinLength();
    }

    private boolean hasNoStopWords(String description, AiAgentProperties.Filtering filtering) {
        Set<String> words = Arrays.stream(description.toLowerCase(Locale.ROOT).split("\\P{L}+"))
                .filter(word -> !word.isBlank())
                .collect(Collectors.toSet());
        return filtering.getStopWords().stream()
                .filter(word -> word != null && !word.isBlank())
                .map(word -> word.trim().toLowerCase(Locale.ROOT))
                .noneMatch(words::contains);
    }

    private boolean hasAllowedAuthor(String author, AiAgentProperties.Filtering filtering) {
        if (author == null || author.isBlank()) {
            return true;
        }

        String normalizedAuthor = author.trim();
        return filtering.getExcludedAuthors().stream()
                .filter(excluded -> excluded != null && !excluded.isBlank())
                .noneMatch(excluded -> excluded.trim().equalsIgnoreCase(normalizedAuthor));
    }

    private void logFiltered(LinkUpdate update, String reason) {
        log.atInfo()
                .addKeyValue("updateId", update.getId())
                .addKeyValue("author", update.getAuthor())
                .log("Update filtered out: {}", reason);
    }
}
