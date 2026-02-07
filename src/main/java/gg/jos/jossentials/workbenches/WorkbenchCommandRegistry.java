package gg.jos.jossentials.workbenches;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class WorkbenchCommandRegistry {
    private final Jossentials plugin;
    private final MessageDispatcher messageDispatcher;
    private final List<Command> registered = new ArrayList<>();

    public WorkbenchCommandRegistry(Jossentials plugin, MessageDispatcher messageDispatcher) {
        this.plugin = plugin;
        this.messageDispatcher = messageDispatcher;
    }

    public void clear() {
        CommandMap commandMap = resolveCommandMap();
        if (commandMap == null) {
            return;
        }
        for (Command command : registered) {
            command.unregister(commandMap);
        }
        registered.clear();
    }

    public void registerConfigured() {
        CommandMap commandMap = resolveCommandMap();
        if (commandMap == null) {
            plugin.getLogger().warning("unable to register workbench commands: command map not found.");
            return;
        }
        for (WorkbenchType type : WorkbenchType.values()) {
            String base = "workbenches." + type.key();
            boolean enabled = plugin.configs().workbenches().getBoolean(base + ".enabled", true);
            if (!enabled) {
                continue;
            }
            List<String> commands = plugin.configs().workbenches().getStringList(base + ".commands");
            if (commands == null || commands.isEmpty()) {
                continue;
            }
            String permission = plugin.configs().workbenches().getString(base + ".permission", "");
            String primary = commands.get(0);
            WorkbenchCommand command = new WorkbenchCommand(plugin, messageDispatcher, primary, type, permission);
            if (commands.size() > 1) {
                command.setAliases(commands.subList(1, commands.size()));
            }
            command.setPermission(permission);
            commandMap.register(plugin.getName().toLowerCase(), command);
            registered.add(command);
        }
    }

    private CommandMap resolveCommandMap() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            Object map = commandMapField.get(Bukkit.getServer());
            if (map instanceof CommandMap commandMap) {
                return commandMap;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
