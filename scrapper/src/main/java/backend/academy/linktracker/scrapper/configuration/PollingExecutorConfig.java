package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.ScrapperPollingProperties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PollingExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService pollingExecutorService(ScrapperPollingProperties pollingProperties) {
        return Executors.newFixedThreadPool(pollingProperties.getWorkerThreads());
    }
}
