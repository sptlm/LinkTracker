package backend.academy.linktracker.scrapper.api.exception;

public class ChatNotFoundException extends RuntimeException {

    public ChatNotFoundException(long chatId) {
        super("Чат с id=%d не найден".formatted(chatId));
    }
}
