package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.client.scrapper.ChatAlreadyExistsException;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClientException;
import backend.academy.linktracker.bot.command.CommandContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotRegistrationService {

    private final UserService userService;
    private final LinkTrackingService linkTrackingService;

    public RegistrationResult ensureRegistered(CommandContext context) {
        RegistrationResult result = userService.registerIfAbsent(context.message());

        try {
            linkTrackingService.registerChat(context.chatId());
        } catch (ChatAlreadyExistsException e) {
            log.atDebug().addKeyValue("chatId", context.chatId()).log("Chat already registered in scrapper");
        } catch (ScrapperClientException e) {
            log.atWarn().setCause(e).addKeyValue("chatId", context.chatId()).log("Failed to register chat in scrapper");
        }

        return result;
    }
}
