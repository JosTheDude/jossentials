package gg.jos.jossentials.warps.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.warps.feature.WarpsFeature;
import org.bukkit.entity.Player;

@CommandAlias("delwarp|deletewarp")
@CommandPermission("jossentials.delwarp")
public final class DelWarpCommand extends BaseCommand {
    private final WarpsFeature warpsFeature;
    private final MessageDispatcher messageDispatcher;

    public DelWarpCommand(WarpsFeature warpsFeature, MessageDispatcher messageDispatcher) {
        this.warpsFeature = warpsFeature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    @CommandCompletion("@warps")
    public void onDelWarp(Player player, String warpName) {
        if (!warpsFeature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        warpsFeature.deleteWarp(player, warpName);
    }
}
