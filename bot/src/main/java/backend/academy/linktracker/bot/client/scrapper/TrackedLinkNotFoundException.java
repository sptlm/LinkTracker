package backend.academy.linktracker.bot.client.scrapper;

public class TrackedLinkNotFoundException extends ScrapperClientException {

    public TrackedLinkNotFoundException(String message) {
        super(message);
    }

    public TrackedLinkNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
