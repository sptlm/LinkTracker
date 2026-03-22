package backend.academy.linktracker.scrapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(excludeName = {
    "org.springframework.boot.data.jpa.autoconfigure.JpaRepositoriesAutoConfiguration",
    "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
})
@ConfigurationPropertiesScan
@EnableScheduling
public class ScrapperApplication {

    static void main(String[] args) {
        SpringApplication.run(ScrapperApplication.class, args);
    }
}
