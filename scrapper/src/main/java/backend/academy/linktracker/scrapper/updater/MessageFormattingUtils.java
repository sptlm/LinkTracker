package backend.academy.linktracker.scrapper.updater;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class MessageFormattingUtils {

    private static final String ELLIPSIS = "...";
    private static final DateTimeFormatter MESSAGE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private MessageFormattingUtils() {}

    public static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    public static String formatInstant(Instant value) {
        return value == null ? "unknown" : MESSAGE_TIME_FORMATTER.format(value);
    }

    public static String truncateWithEllipsis(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }

        if (maxLength <= ELLIPSIS.length()) {
            return value.substring(0, maxLength);
        }

        return value.substring(0, maxLength - ELLIPSIS.length()) + ELLIPSIS;
    }
}
