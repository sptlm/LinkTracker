package backend.academy.linktracker.bot.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class BotMetrics {

    private static final double[] DURATION_BUCKETS_MS = {5, 10, 25, 50, 100, 250, 500, 1_000, 2_500, 5_000, 10_000};

    private final MeterRegistry meterRegistry;
    private final Counter sentNotifications;
    private final Map<String, Counter> commandCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> telegramRequestCounters = new ConcurrentHashMap<>();
    private final Map<String, DistributionSummary> commandDurationSummaries = new ConcurrentHashMap<>();
    private final Map<String, DistributionSummary> commandHandlingDurationSummaries = new ConcurrentHashMap<>();

    public BotMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.sentNotifications = Counter.builder("sent_notification")
                .description("Sent Telegram notifications")
                .register(meterRegistry);
    }

    public void recordCommandRequest(String command) {
        commandCounters
                .computeIfAbsent(command, key -> Counter.builder("command_requests")
                        .description("Handled bot commands")
                        .tag("command", key)
                        .register(meterRegistry))
                .increment();
    }

    public void recordTelegramRequest(String requestType) {
        telegramRequestCounters
                .computeIfAbsent(requestType, key -> Counter.builder("telegram_requests")
                        .description("Telegram updates received by request type")
                        .tag("request_type", key)
                        .register(meterRegistry))
                .increment();
    }

    public void recordCommandDuration(String scope, String scopeType, long startedAtNanos) {
        recordDuration(commandDurationSummaries, "command_duration_ms_total", scope, scopeType, startedAtNanos);
    }

    public void recordCommandHandlingDuration(String command, long startedAtNanos) {
        double elapsedMs = elapsedMs(startedAtNanos);
        commandHandlingDurationSummaries
                .computeIfAbsent(command, key -> DistributionSummary.builder("command_handling_duration_ms_total")
                        .description("Bot command handling duration in milliseconds")
                        .baseUnit("milliseconds")
                        .tag("command", key)
                        .serviceLevelObjectives(DURATION_BUCKETS_MS)
                        .publishPercentileHistogram()
                        .register(meterRegistry))
                .record(elapsedMs);
    }

    public void recordSentNotification() {
        sentNotifications.increment();
    }

    private void recordDuration(
            Map<String, DistributionSummary> summaries,
            String metricName,
            String scope,
            String scopeType,
            long startedAtNanos) {
        double elapsedMs = elapsedMs(startedAtNanos);
        summaries
                .computeIfAbsent(durationKey(scope, scopeType), ignored -> DistributionSummary.builder(metricName)
                        .description("Bot operation duration in milliseconds")
                        .baseUnit("milliseconds")
                        .tag("scope", scope)
                        .tag("scope_type", scopeType)
                        .serviceLevelObjectives(DURATION_BUCKETS_MS)
                        .publishPercentileHistogram()
                        .register(meterRegistry))
                .record(elapsedMs);
    }

    private static double elapsedMs(long startedAtNanos) {
        return (double) (System.nanoTime() - startedAtNanos) / TimeUnit.MILLISECONDS.toNanos(1);
    }

    private static String durationKey(String scope, String scopeType) {
        return scope + ":" + scopeType;
    }
}
