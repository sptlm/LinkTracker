package backend.academy.linktracker.scrapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@Import(TestcontainersConfiguration.class)
public class TestScrapperApplication {

    static void main(String[] args) {
        SpringApplication.from(ScrapperApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
