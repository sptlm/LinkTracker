package backend.academy.linktracker.scrapper.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications")
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class NotificationTransportProperties {

    private NotificationTransport transport = NotificationTransport.KAFKA;
}
