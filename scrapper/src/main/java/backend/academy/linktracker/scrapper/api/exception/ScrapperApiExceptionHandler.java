package backend.academy.linktracker.scrapper.api.exception;

import backend.academy.linktracker.scrapper.generated.dto.ApiErrorResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ScrapperApiExceptionHandler {

    @ExceptionHandler(ChatAlreadyRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleChatAlreadyRegistered(ChatAlreadyRegisteredException e) {
        return build(HttpStatus.CONFLICT, "Чат уже существует", e);
    }

    @ExceptionHandler(ChatNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleChatNotFound(ChatNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, "Чат не существует", e);
    }

    @ExceptionHandler(LinkAlreadyTrackedException.class)
    public ResponseEntity<ApiErrorResponse> handleLinkAlreadyTracked(LinkAlreadyTrackedException e) {
        return build(HttpStatus.CONFLICT, "Ссылка уже отслеживается", e);
    }

    @ExceptionHandler(TagAlreadyAssignedException.class)
    public ResponseEntity<ApiErrorResponse> handleTagAlreadyAssigned(TagAlreadyAssignedException e) {
        return build(HttpStatus.CONFLICT, "Тег уже назначен", e);
    }

    @ExceptionHandler({TrackedLinkNotFoundException.class, TagNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleTrackedLinkNotFound(RuntimeException e) {
        return build(HttpStatus.NOT_FOUND, "Чат не существует или ссылка не найдена", e);
    }

    @ExceptionHandler({
        UnsupportedLinkException.class,
        InvalidRequestException.class,
        MethodArgumentNotValidException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception e) {
        return build(HttpStatus.BAD_REQUEST, "Некорректные параметры запроса", e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception e) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервиса", e);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String description, Exception e) {
        ApiErrorResponse body = new ApiErrorResponse()
                .description(description)
                .code(status.name())
                .exceptionName(e.getClass().getSimpleName())
                .exceptionMessage(e.getMessage())
                .stacktrace(List.of());
        return ResponseEntity.status(status).body(body);
    }
}
