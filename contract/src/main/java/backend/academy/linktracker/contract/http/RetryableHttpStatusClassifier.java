package backend.academy.linktracker.contract.http;

import java.util.Set;

public class RetryableHttpStatusClassifier {

    private final Set<Integer> retryableStatuses;

    public RetryableHttpStatusClassifier(Set<Integer> retryableStatuses) {
        this.retryableStatuses = Set.copyOf(retryableStatuses);
    }

    public boolean isRetryable(int statusCode) {
        return retryableStatuses.contains(statusCode);
    }
}
