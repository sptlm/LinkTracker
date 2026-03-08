package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.BotClientProperties;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class ExternalClientsConfig {

    @Bean
    public RestClient githubRestClient(RestClient.Builder builder, GithubProperties githubProperties) {
        RestClient.Builder clientBuilder = builder
                .baseUrl(githubProperties.getBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        if (StringUtils.hasText(githubProperties.getToken())) {
            clientBuilder.defaultHeader("Authorization", "Bearer " + githubProperties.getToken());
        }

        return clientBuilder.build();
    }

    @Bean
    public RestClient stackOverflowRestClient(RestClient.Builder builder, StackoverflowProperties stackoverflowProperties) {
        return builder
                .baseUrl(stackoverflowProperties.getBaseUrl())
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public RestClient botRestClient(RestClient.Builder builder, BotClientProperties properties) {
        return builder
                .baseUrl(properties.getBotBaseUrl())
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
