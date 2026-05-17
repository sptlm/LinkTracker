package backend.academy.linktracker.scrapper.api.exception;

public class RetryableHttpStatusException extends ExternalServiceException {

    private final int statusCode;

    public RetryableHttpStatusException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
