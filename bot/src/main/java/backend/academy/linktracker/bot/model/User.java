package backend.academy.linktracker.bot.model;

import java.time.Instant;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@EqualsAndHashCode(of = "userId")
public class User {

    private final long userId;

    private final String username;

    private final String firstName;

    private final String lastName;

    private final Instant registeredAt;

    public String displayName() {
        if (firstName != null && !firstName.isBlank()) {
            return firstName;
        }
        if (username != null && !username.isBlank()) {
            return "@" + username;
        }
        return "пользователь";
    }
}
