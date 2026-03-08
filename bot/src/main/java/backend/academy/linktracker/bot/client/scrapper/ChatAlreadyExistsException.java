package backend.academy.linktracker.bot.client.scrapper;

public class ChatAlreadyExistsException extends ScrapperClientException {

    public ChatAlreadyExistsException(String message) {
        super(message);
    }

    public ChatAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
