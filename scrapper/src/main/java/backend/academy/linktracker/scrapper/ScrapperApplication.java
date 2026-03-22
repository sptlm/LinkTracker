package backend.academy.linktracker.scrapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(excludeName = {
    "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
@ConfigurationPropertiesScan
@EnableScheduling
public class ScrapperApplication {

    static void main(String[] args) {
        SpringApplication.run(ScrapperApplication.class, args);
    }
}
