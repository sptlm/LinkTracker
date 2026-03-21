package backend.academy.linktracker.bot.command;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CommandRegistry {

    private final Map<String, Command> commandMap;

    public CommandRegistry(List<Command> commands) {
        this.commandMap =
                commands.stream().collect(Collectors.toUnmodifiableMap(Command::command, Function.identity()));
    }

    public Optional<Command> find(String commandName) {
        return Optional.ofNullable(commandMap.get(commandName));
    }

    public List<Command> getCommands() {
        return commandMap.values().stream()
                .sorted(Comparator.comparing(Command::command))
                .toList();
    }
}
