package gg.jos.jossentials.workbenches;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.util.ColorUtil;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class WorkbenchCommand extends Command {
    private final Jossentials plugin;
    private final MessageDispatcher messageDispatcher;
    private final WorkbenchType type;
    private final String permission;

    public WorkbenchCommand(Jossentials plugin, MessageDispatcher messageDispatcher, String name, WorkbenchType type, String permission) {
        super(name);
        this.plugin = plugin;
        this.messageDispatcher = messageDispatcher;
        this.type = type;
        this.permission = permission;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.mini("<red>Only players can use this command."));
            return true;
        }
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            messageDispatcher.send(player, "messages.no-permission", "<red>You do not have permission.");
            return true;
        }
        plugin.scheduler().runEntity(player, () -> type.open(player));
        return true;
    }
}
