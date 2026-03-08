package backend.academy.linktracker.bot.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.hibernate.validator.constraints.URL;

public record LinkUpdateRequest(
        @NotNull @Positive Long id,
        @NotBlank @URL String url,
        @NotBlank String description,
        @NotEmpty List<@NotNull @Positive Long> tgChatIds
) {}
