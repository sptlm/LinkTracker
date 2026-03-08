package backend.academy.linktracker.scrapper.api.exception;

public class ChatAlreadyRegisteredException extends RuntimeException {

    public ChatAlreadyRegisteredException(long chatId) {
        super("Чат с id=%d уже зарегистрирован".formatted(chatId));
    }
}
