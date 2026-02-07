package gg.jos.jossentials.spawn.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import gg.jos.jossentials.spawn.SpawnFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.entity.Player;

@CommandAlias("spawn")
@CommandPermission("jossentials.spawn")
public final class SpawnCommand extends BaseCommand {
    private final SpawnFeature feature;
    private final MessageDispatcher messageDispatcher;

    public SpawnCommand(SpawnFeature feature, MessageDispatcher messageDispatcher) {
        this.feature = feature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    public void onSpawn(Player player) {
        if (!feature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        feature.teleport(player);
    }
}
