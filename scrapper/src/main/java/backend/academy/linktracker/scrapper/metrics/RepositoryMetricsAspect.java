package backend.academy.linktracker.scrapper.metrics;

import java.util.Locale;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RepositoryMetricsAspect {

    private static final String REPOSITORY_PACKAGE = "backend.academy.linktracker.scrapper.repository";

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
            metrics.recordRequestDuration("database", repositoryOperation(joinPoint), startedAt);
        }
    }

    private String repositoryOperation(ProceedingJoinPoint joinPoint) {
        return repositoryName(joinPoint) + "#" + joinPoint.getSignature().getName();
    }

    private String repositoryName(ProceedingJoinPoint joinPoint) {
        return repositoryName(joinPoint.getSignature().getDeclaringType())
                .or(() -> joinPoint.getTarget() == null
                        ? Optional.empty()
                        : repositoryName(joinPoint.getTarget().getClass()))
                .orElse("unknown");
    }

    private Optional<String> repositoryName(Class<?> type) {
        if (type == null || Object.class.equals(type)) {
            return Optional.empty();
        }

        if (isProjectRepository(type) && !type.getSimpleName().isBlank()) {
            return Optional.of(normalizeRepositoryName(type.getSimpleName()));
        }

        for (Class<?> interfaceType : type.getInterfaces()) {
            Optional<String> name = repositoryName(interfaceType);
            if (name.isPresent()) {
                return name;
            }
        }

        return repositoryName(type.getSuperclass());
    }

    private boolean isProjectRepository(Class<?> type) {
        Package typePackage = type.getPackage();
        return typePackage != null && typePackage.getName().startsWith(REPOSITORY_PACKAGE);
    }

    private String normalizeRepositoryName(String simpleName) {
        return simpleName
                .replace("Repository", "")
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT);
    }
}
