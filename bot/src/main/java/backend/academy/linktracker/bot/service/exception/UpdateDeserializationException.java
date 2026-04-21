package backend.academy.linktracker.bot.service.exception;

public class UpdateDeserializationException extends RuntimeException {

    public UpdateDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
