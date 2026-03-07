package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.properties.BotMessagesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotMessagesService {

    private final BotMessagesProperties props;

    public String unknownCommand() {
        return props.getUnknownCommand();
    }

    public String helpHeader() {
        return props.getHelpHeader();
    }

    public String startWelcome(String displayName) {
        return props.getStartWelcome().formatted(displayName);
    }

    public String welcomeBack(String displayName) {
        return props.getWelcomeBack().formatted(displayName);
    }
}
