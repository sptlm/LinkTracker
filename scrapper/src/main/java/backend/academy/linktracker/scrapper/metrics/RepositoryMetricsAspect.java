package backend.academy.linktracker.scrapper.metrics;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RepositoryMetricsAspect {

    private final ScrapperMetrics metrics;

    public RepositoryMetricsAspect(ScrapperMetrics metrics) {
        this.metrics = metrics;
    }

    @Around("within(backend.academy.linktracker.scrapper.repository..*)")
    public Object recordRepositoryDuration(ProceedingJoinPoint joinPoint) throws Throwable {
        long startedAt = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            metrics.recordRequestDuration("database", repositoryName(joinPoint), startedAt);
        }
    }

    private String repositoryName(ProceedingJoinPoint joinPoint) {
        String simpleName = joinPoint.getSignature().getDeclaringType().getSimpleName();
        return simpleName.replace("Repository", "").toLowerCase(java.util.Locale.ROOT);
    }
}
