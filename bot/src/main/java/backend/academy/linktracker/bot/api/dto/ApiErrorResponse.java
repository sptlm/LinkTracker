package backend.academy.linktracker.bot.api.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record ApiErrorResponse(
        String description, String code, String exceptionName, String exceptionMessage, List<String> stacktrace) {}
