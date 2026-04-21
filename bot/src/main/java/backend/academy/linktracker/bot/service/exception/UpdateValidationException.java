package backend.academy.linktracker.bot.service.exception;

public class UpdateValidationException extends RuntimeException {

    public UpdateValidationException(String message) {
        super(message);
    }
}
