package backend.academy.linktracker.bot.api;

import backend.academy.linktracker.bot.generated.dto.ApiErrorResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BotApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException e) {
        return buildBadRequest(e);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleUnreadableBody(HttpMessageNotReadableException e) {
        return buildBadRequest(e);
    }

    private ApiErrorResponse buildBadRequest(Exception exception) {
        return new ApiErrorResponse()
                .description("Некорректные параметры запроса")
                .code(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                .exceptionName(exception.getClass().getSimpleName())
                .exceptionMessage(exception.getMessage())
                .stacktrace(List.of());
    }
}
