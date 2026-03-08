package backend.academy.linktracker.bot.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SupportedLinkParser {

    private static final Pattern GITHUB_REPOSITORY = Pattern.compile("^/[^/]+/[^/]+/?$");
    private static final Pattern STACKOVERFLOW_QUESTION = Pattern.compile("^/questions/\\d+(?:/[^/]+)?/?$");

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
        String path = uri.getPath() == null ? "" : uri.getPath();

        if ("github.com".equals(normalizedHost)) {
            return GITHUB_REPOSITORY.matcher(path).matches();
        }

        return ("stackoverflow.com".equals(normalizedHost) || "www.stackoverflow.com".equals(normalizedHost))
                && STACKOVERFLOW_QUESTION.matcher(path).matches();
    }
}
