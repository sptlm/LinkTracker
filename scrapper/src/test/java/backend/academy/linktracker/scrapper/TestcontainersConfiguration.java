package backend.academy.linktracker.scrapper;

import java.io.File;
import java.nio.file.Path;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.ImageFromDockerfile;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final Network NETWORK = Network.newNetwork();

    private static Path findJar(String moduleName) {
        String basePath = new File(moduleName).exists() ? "./" : "../";
        File targetDir = new File(basePath + moduleName + "/target/");
        File[] jars = targetDir.listFiles((dir, name) -> name.endsWith(".jar")
                && !name.endsWith("-plain.jar")
                && !name.endsWith("-javadoc.jar")
                && !name.endsWith("-sources.jar"));
        if (jars == null || jars.length == 0) {
            throw new RuntimeException(
                    "JAR file for " + moduleName + " not found. Run 'mvn clean package -DskipTests'");
        }
        return jars[0].toPath();
    }

    @Bean
    public GenericContainer<?> scrapperContainer() {
        return new GenericContainer<>(new ImageFromDockerfile()
                        .withFileFromPath("app.jar", findJar("scrapper"))
                        .withDockerfileFromBuilder(builder -> builder.from("openjdk:25-ea-slim")
                                .copy("app.jar", "/app.jar")
                                .entryPoint("java", "--enable-preview", "-jar", "/app.jar")
                                .build()))
                .withNetwork(NETWORK)
                .withNetworkAliases("scrapper")
                .withExposedPorts(8081)
                .withAccessToHost(true)
                .withEnv("SCRAPPER_NOTIFICATION_TRANSPORT", "HTTP")
                .withEnv("SCRAPPER_DATASOURCE_URL", "jdbc:postgresql://host.testcontainers.internal:5433/linktracker")
                .withEnv("SCRAPPER_DATASOURCE_USERNAME", "postgres")
                .withEnv("SCRAPPER_DATASOURCE_PASSWORD", "postgres")
                .withEnv("GITHUB_TOKEN", "mock")
                .withEnv("STACKOVERFLOW_KEY", "mock")
                .withEnv("STACKOVERFLOW_ACCESS_KEY", "mock");
    }

    @Bean
    public GenericContainer<?> botContainer() {
        return new GenericContainer<>(new ImageFromDockerfile()
                        .withFileFromPath("app.jar", findJar("bot"))
                        .withDockerfileFromBuilder(builder -> builder.from("openjdk:25-ea-slim")
                                .copy("app.jar", "/app.jar")
                                .entryPoint("java", "--enable-preview", "-jar", "/app.jar")
                                .build()))
                .withNetwork(NETWORK)
                .withNetworkAliases("bot")
                .withExposedPorts(8080)
                .withEnv("BOT_NOTIFICATION_TRANSPORT", "HTTP")
                .withEnv("APP_SCRAPPER_BASE_URL", "http://scrapper:8081")
                .withEnv("TELEGRAM_TOKEN", "mock_token");
    }
}
