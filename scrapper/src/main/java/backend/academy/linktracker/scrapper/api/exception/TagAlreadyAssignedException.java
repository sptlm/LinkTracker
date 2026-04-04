package backend.academy.linktracker.scrapper.api.exception;

public class TagAlreadyAssignedException extends RuntimeException {

    public TagAlreadyAssignedException(String tag, String url, long chatId) {
        super("Тег %s уже назначен ссылке %s для чата %d".formatted(tag, url, chatId));
    }
}
