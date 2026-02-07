package gg.jos.jossentials.workbenches.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.Default;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.workbenches.WorkbenchType;
import org.bukkit.entity.Player;

public abstract class WorkbenchCommandBase extends BaseCommand {
    protected final Jossentials plugin;
    protected final MessageDispatcher messageDispatcher;
    protected final WorkbenchType type;
    protected final String configKey;

    protected WorkbenchCommandBase(Jossentials plugin, MessageDispatcher messageDispatcher, WorkbenchType type) {
        this.plugin = plugin;
        this.messageDispatcher = messageDispatcher;
        this.type = type;
        this.configKey = "workbenches." + type.key();
    }

    @Default
    public void onOpen(Player player) {
        String permission = plugin.configs().workbenches().getString(configKey + ".permission", "");
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            messageDispatcher.send(player, "messages.no-permission", "<red>You do not have permission.");
            return;
        }
        plugin.scheduler().runEntity(player, () -> type.open(player));
    }
}
