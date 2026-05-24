package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.contract.http.RetryableHttpStatusClassifier;
import backend.academy.linktracker.scrapper.properties.BotClientProperties;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.HttpClientProperties;
import backend.academy.linktracker.scrapper.properties.ResilienceHttpProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class ExternalClientsConfig {

    @Bean
    public RestClient githubRestClient(
            RestClient.Builder builder, GithubProperties githubProperties, HttpClientProperties httpProperties) {
        RestClient.Builder clientBuilder = builder.baseUrl(githubProperties.getBaseUrl())
                .requestFactory(requestFactory(httpProperties))
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        if (StringUtils.hasText(githubProperties.getToken())) {
            clientBuilder.defaultHeader("Authorization", "Bearer " + githubProperties.getToken());
        }

        return clientBuilder.build();
    }

    @Bean
    public RestClient stackOverflowRestClient(
            RestClient.Builder builder,
            StackoverflowProperties stackoverflowProperties,
            HttpClientProperties httpProperties) {
        return builder.baseUrl(stackoverflowProperties.getBaseUrl())
                .requestFactory(requestFactory(httpProperties))
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public RestClient botRestClient(
            RestClient.Builder builder, BotClientProperties properties, HttpClientProperties httpProperties) {
        return builder.baseUrl(properties.getBotBaseUrl())
                .requestFactory(requestFactory(httpProperties))
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public RetryableHttpStatusClassifier retryableHttpStatusClassifier(ResilienceHttpProperties properties) {
        return new RetryableHttpStatusClassifier(properties.getRetryableStatuses());
    }

    private JdkClientHttpRequestFactory requestFactory(HttpClientProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return requestFactory;
    }
}
