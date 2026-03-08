package backend.academy.linktracker.bot.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SupportedLinkParser {

    public Optional<URI> parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }

        try {
            URI uri = new URI(rawText.trim());
            if (!isSupported(uri)) {
                return Optional.empty();
            }
            return Optional.of(uri);
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    private boolean isSupported(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return false;
        }

        String[] segments = path.split("/");
        // первый элемент всегда "", потому что путь начинается с "/"

        if ("github.com".equals(normalizedHost)) {
            // Ожидаем /{owner}/{repo} или /{owner}/{repo}/
            // то есть сегменты: ["", owner, repo] или ["", owner, repo, ""]
            if (segments.length == 3 || (segments.length == 4 && segments[3].isEmpty())) {
                return !segments[1].isEmpty() && !segments[2].isEmpty();
            }
            return false;
        }

        if ("stackoverflow.com".equals(normalizedHost) || "www.stackoverflow.com".equals(normalizedHost)) {
            // Ожидаем /questions/{id} или /questions/{id}/{slug}[ /]
            if (segments.length < 3) {
                return false;
            }
            if (!"questions".equals(segments[1])) {
                return false;
            }
            // segments[2] должен быть числовым id
            return isNumeric(segments[2]);
        }

        return false;
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
