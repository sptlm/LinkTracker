package backend.academy.linktracker.bot.client.scrapper;

public class RetryableScrapperClientException extends ScrapperClientException {

    private final int statusCode;

    public RetryableScrapperClientException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
