package backend.academy.linktracker.bot.client.scrapper;

public class DuplicateLinkException extends ScrapperClientException {

    public DuplicateLinkException(String message) {
        super(message);
    }

    public DuplicateLinkException(String message, Throwable cause) {
        super(message, cause);
    }
}
