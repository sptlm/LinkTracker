package backend.academy.linktracker.bot.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record LinkUpdateRequest(
    long id,
    @NotBlank String url,
    String description,
    @NotEmpty List<Long> tgChatIds
) {}
