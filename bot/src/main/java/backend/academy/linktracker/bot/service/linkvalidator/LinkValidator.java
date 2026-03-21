package backend.academy.linktracker.bot.service.linkvalidator;

import java.net.URI;

public interface LinkValidator {

    boolean supports(URI uri);

    boolean isValid(URI uri);
}
