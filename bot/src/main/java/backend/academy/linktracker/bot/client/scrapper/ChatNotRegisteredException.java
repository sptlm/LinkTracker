package backend.academy.linktracker.bot.client.scrapper;

public class ChatNotRegisteredException extends ScrapperClientException {

    public ChatNotRegisteredException(String message) {
        super(message);
    }

    public ChatNotRegisteredException(String message, Throwable cause) {
        super(message, cause);
    }
}
