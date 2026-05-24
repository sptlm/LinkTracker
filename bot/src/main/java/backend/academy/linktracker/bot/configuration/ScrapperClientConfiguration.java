package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.properties.HttpClientProperties;
import backend.academy.linktracker.bot.properties.ResilienceHttpProperties;
import backend.academy.linktracker.bot.properties.ScrapperProperties;
import backend.academy.linktracker.contract.http.RetryableHttpStatusClassifier;
import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ScrapperClientConfiguration {

    @Bean
    public RestClient scrapperRestClient(ScrapperProperties properties, HttpClientProperties httpProperties) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory(httpProperties))
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
