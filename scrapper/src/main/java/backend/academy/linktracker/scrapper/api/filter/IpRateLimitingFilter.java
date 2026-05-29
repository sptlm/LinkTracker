package backend.academy.linktracker.scrapper.api.filter;

import backend.academy.linktracker.scrapper.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.properties.RateLimitProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class IpRateLimitingFilter extends OncePerRequestFilter {

    private static final String TG_CHAT_ID_HEADER = "Tg-Chat-Id";

    private final RateLimitProperties properties;
    private final ScrapperMetrics metrics;
    private final Cache<String, Bucket> buckets;

    public IpRateLimitingFilter(RateLimitProperties properties, ScrapperMetrics metrics) {
        this.properties = properties;
        this.metrics = metrics;
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(properties.getCacheExpireAfterAccess())
                .maximumSize(properties.getCacheMaximumSize())
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator")
                || request.getRequestURI().startsWith("/metrics");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        metrics.recordApiRequest(requestSource(request));

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.get(rateLimitKey(request), ignored -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000)));
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(properties.getCapacity())
                .refillGreedy(properties.getRefillTokens(), properties.getRefillPeriod())
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String rateLimitKey(HttpServletRequest request) {
        String chatId = request.getHeader(TG_CHAT_ID_HEADER);
        if (chatId != null && !chatId.isBlank()) {
            return "tg-chat-id:" + chatId.trim();
        }
        return "remote-addr:" + request.getRemoteAddr();
    }

    private String requestSource(HttpServletRequest request) {
        String chatId = request.getHeader(TG_CHAT_ID_HEADER);
        if (chatId != null && !chatId.isBlank()) {
            return "bot";
        }
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.toLowerCase(java.util.Locale.ROOT).contains("prometheus")) {
            return "prometheus";
        }
        return "unknown";
    }
}
