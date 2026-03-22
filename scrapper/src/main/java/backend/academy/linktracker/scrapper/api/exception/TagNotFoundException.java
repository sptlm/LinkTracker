package backend.academy.linktracker.scrapper.api.exception;

public class TagNotFoundException extends RuntimeException {

    public TagNotFoundException(String tag, String url, long chatId) {
        super("Тег %s не найден у ссылки %s для чата %d".formatted(tag, url, chatId));
    }
}
