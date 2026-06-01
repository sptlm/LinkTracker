package backend.academy.linktracker.scrapper.metrics;

import backend.academy.linktracker.scrapper.model.LinkSourceType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class ScrapperMetrics {

    private static final double[] DURATION_BUCKETS_MS = {5, 10, 25, 50, 100, 250, 500, 1_000, 2_500, 5_000, 10_000};

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> apiRequestCounters = new ConcurrentHashMap<>();
    private final Map<String, DistributionSummary> durationSummaries = new ConcurrentHashMap<>();
    private final Map<LinkSourceType, AtomicLong> trackedLinks = new EnumMap<>(LinkSourceType.class);

    public ScrapperMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        for (LinkSourceType type : LinkSourceType.values()) {
            AtomicLong count = new AtomicLong();
            trackedLinks.put(type, count);
            Gauge.builder("links_on_track_total", count, AtomicLong::get)
                    .description("Number of active links stored for monitoring")
                    .tag("tracked_source", sourceName(type))
                    .register(meterRegistry);
        }
    }

    public void recordApiRequest(String source) {
        apiRequestCounters
                .computeIfAbsent(source, key -> Counter.builder("api_requests")
                        .description("Incoming Scrapper API requests")
                        .tag("source", key)
                        .register(meterRegistry))
                .increment();
    }

    public void recordRequestDuration(String scope, String scopeType, long startedAtNanos) {
        double elapsedMs = (double) (System.nanoTime() - startedAtNanos) / TimeUnit.MILLISECONDS.toNanos(1);
        durationSummaries
                .computeIfAbsent(durationKey(scope, scopeType), ignored -> DistributionSummary.builder(
                                "request_duration_ms_total")
                        .description("Scrapper operation duration in milliseconds")
                        .baseUnit("milliseconds")
                        .tag("scope", scope)
                        .tag("scope_type", scopeType)
                        .serviceLevelObjectives(DURATION_BUCKETS_MS)
                        .publishPercentileHistogram()
                        .register(meterRegistry))
                .record(elapsedMs);
    }

    public void recordTrackedLinkCreated(LinkSourceType type) {
        AtomicLong count = trackedLinks.get(type);
        if (count != null) {
            count.incrementAndGet();
        }
    }

    public void recordTrackedLinkDeleted(LinkSourceType type) {
        AtomicLong count = trackedLinks.get(type);
        if (count != null) {
            count.updateAndGet(value -> Math.max(0, value - 1));
        }
    }

    public void setTrackedLinks(LinkSourceType type, long value) {
        AtomicLong count = trackedLinks.get(type);
        if (count != null) {
            count.set(Math.max(0, value));
        }
    }

    private static String durationKey(String scope, String scopeType) {
        return scope + ":" + scopeType;
    }

    private static String sourceName(LinkSourceType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }
}
