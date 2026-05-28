package backend.academy.linktracker.ai.service;

public enum UpdatePriority {
    LOW,
    MEDIUM,
    HIGH;

    public static UpdatePriority max(UpdatePriority first, UpdatePriority second) {
        return first.ordinal() >= second.ordinal() ? first : second;
    }
}
