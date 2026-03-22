package backend.academy.linktracker.bot.command.impl;

import backend.academy.linktracker.bot.client.scrapper.ScrapperClientException;
import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandContext;
import backend.academy.linktracker.bot.service.BotMessagesService;
import backend.academy.linktracker.bot.service.LinkTrackingService;
import backend.academy.linktracker.scrapper.generated.dto.LinksPost200Response;
import backend.academy.linktracker.scrapper.generated.dto.ListLinksResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListCommand implements Command {

    private final LinkTrackingService linkTrackingService;
    private final BotMessagesService messages;

    @Override
    public String command() {
        return "/list";
    }

    @Override
    public String description() {
        return "Показать отслеживаемые ссылки";
    }

    @Override
    public void handle(CommandContext context) {
        String tag = extractOptionalTag(context.messageText());

        try {
            ListLinksResponse response = linkTrackingService.getLinks(context.chatId());
            List<LinksPost200Response> links = response.getLinks() == null ? List.of() : response.getLinks();

            if (tag != null) {
                links = links.stream()
                        .filter(link -> link.getTags() != null && link.getTags().contains(tag))
                        .toList();
            }

            if (links.isEmpty()) {
                context.reply(messages.noTrackedLinks());
                return;
            }

            String text = links.stream().map(this::formatLink).collect(Collectors.joining("\n\n"));
            context.reply(text);
        } catch (ScrapperClientException e) {
            log.atWarn().setCause(e).addKeyValue("chatId", context.chatId()).log("Failed to get links from scrapper");
            context.reply(messages.scrapperUnavailable());
        }
    }

    private String extractOptionalTag(String messageText) {
        String[] parts = messageText.trim().split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            return null;
        }
        return parts[1].trim();
    }

    private String formatLink(LinksPost200Response link) {
        StringBuilder sb = new StringBuilder(link.getUrl().toString());

        if (link.getTags() != null && !link.getTags().isEmpty()) {
            sb.append("\nТеги: ").append(String.join(", ", link.getTags()));
        }

        return sb.toString();
    }
}
