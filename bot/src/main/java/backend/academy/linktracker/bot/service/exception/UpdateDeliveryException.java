package backend.academy.linktracker.bot.service.exception;

public class UpdateDeliveryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UpdateDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
