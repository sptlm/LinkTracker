package backend.academy.linktracker.scrapper.configuration;

import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.MappingSocketAddressResolver;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValkeyLettuceConfiguration {

    private static final String DOCKER_HOST_GATEWAY = "host.docker.internal";
    private static final String LOCALHOST = "localhost";

    @Bean(destroyMethod = "shutdown")
    public ClientResources valkeyClientResources() {
        return DefaultClientResources.builder()
                .socketAddressResolver(MappingSocketAddressResolver.create(ValkeyLettuceConfiguration::mapAddress))
                .build();
    }

    @Bean
    public LettuceClientConfigurationBuilderCustomizer valkeyClientResourcesCustomizer(
            ClientResources valkeyClientResources) {
        return builder -> builder.clientResources(valkeyClientResources);
    }

    private static HostAndPort mapAddress(HostAndPort address) {
        if (DOCKER_HOST_GATEWAY.equalsIgnoreCase(address.getHostText())) {
            return HostAndPort.of(LOCALHOST, address.getPort());
        }
        return address;
    }
}
