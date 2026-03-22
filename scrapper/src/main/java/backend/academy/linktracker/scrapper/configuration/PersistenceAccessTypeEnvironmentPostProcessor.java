package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.AccessType;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class PersistenceAccessTypeEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "scrapperPersistenceAccessType";

    private static final String JPA_AUTO_CONFIGURATION_EXCLUDES = String.join(",",
            "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        AccessType accessType = environment.getProperty("app.persistence.access-type", AccessType.class, AccessType.SQL);
        if (accessType != AccessType.SQL) {
            return;
        }

        environment.getPropertySources().addFirst(new MapPropertySource(
                PROPERTY_SOURCE_NAME,
                Map.of("spring.autoconfigure.exclude", JPA_AUTO_CONFIGURATION_EXCLUDES)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
