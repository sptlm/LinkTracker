package backend.academy.linktracker.bot.service.linkvalidator;

import java.net.URI;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class GithubLinkValidator implements LinkValidator {

    @Override
    public boolean supports(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            return false;
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return "github.com".equals(normalizedHost) || "www.github.com".equals(normalizedHost);
    }

    @Override
    public boolean isValid(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return false;
        }

        String[] segments = path.split("/");
        return (segments.length == 3 || (segments.length == 4 && segments[3].isEmpty()))
                && !segments[1].isEmpty()
                && !segments[2].isEmpty();
    }
}
