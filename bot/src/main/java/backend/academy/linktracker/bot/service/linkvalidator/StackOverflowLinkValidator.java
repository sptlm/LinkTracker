package backend.academy.linktracker.bot.service.linkvalidator;

import java.net.URI;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class StackOverflowLinkValidator implements LinkValidator {

    @Override
    public boolean supports(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            return false;
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return "stackoverflow.com".equals(normalizedHost) || "www.stackoverflow.com".equals(normalizedHost);
    }

    @Override
    public boolean isValid(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return false;
        }

        String[] segments = path.split("/");
        return segments.length >= 3 && "questions".equals(segments[1]) && isNumeric(segments[2]);
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
