package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.properties.ResilienceHttpProperties;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RetryableHttpStatusClassifier {

    private final Set<Integer> retryableStatuses;

    @Autowired
    public RetryableHttpStatusClassifier(ResilienceHttpProperties properties) {
        this.retryableStatuses = Set.copyOf(properties.getRetryableStatuses());
    }

    public RetryableHttpStatusClassifier(Set<Integer> retryableStatuses) {
        this.retryableStatuses = Set.copyOf(retryableStatuses);
    }

    public boolean isRetryable(int statusCode) {
        return retryableStatuses.contains(statusCode);
    }
}
