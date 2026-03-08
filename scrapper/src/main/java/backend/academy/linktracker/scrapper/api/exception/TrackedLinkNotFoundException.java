package backend.academy.linktracker.scrapper.api.exception;

public class TrackedLinkNotFoundException extends RuntimeException {

    public TrackedLinkNotFoundException(String url, long chatId) {
        super("Ссылка %s не найдена для чата %d".formatted(url, chatId));
    }
}
