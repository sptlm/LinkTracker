package backend.academy.linktracker.bot.command;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CommandDispatcher {

    private static final String UNKNOWN_COMMAND_RESPONSE =
            "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд.";

    private final Map<String, Command> commandMap;

    public CommandDispatcher(List<Command> commands) {
        this.commandMap = commands.stream().collect(Collectors.toMap(Command::command, Function.identity()));
    }

    public List<Command> getCommands() {
        return List.copyOf(commandMap.values());
    }

    public String dispatch(CommandContext context) {
        String commandText = context.messageText().split("\\s+")[0];
        Command command = commandMap.get(commandText);

        if (command == null) {
            return UNKNOWN_COMMAND_RESPONSE;
        }
        return command.handle(context);
    }
}
