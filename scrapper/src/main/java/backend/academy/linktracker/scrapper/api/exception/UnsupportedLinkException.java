package backend.academy.linktracker.scrapper.api.exception;

public class UnsupportedLinkException extends RuntimeException {

    public UnsupportedLinkException(String link) {
        super("Ссылка не поддерживается: " + link);
    }
}
