package backend.academy.linktracker.scrapper.api.exception;

public class LinkAlreadyTrackedException extends RuntimeException {

    public LinkAlreadyTrackedException(String url, long chatId) {
        super("Ссылка %s уже отслеживается для чата %d".formatted(url, chatId));
    }
}
