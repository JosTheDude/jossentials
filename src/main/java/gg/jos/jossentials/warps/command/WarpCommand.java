package gg.jos.jossentials.warps.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.warps.feature.WarpsFeature;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("warp")
@CommandPermission("jossentials.warp")
public final class WarpCommand extends BaseCommand {
    private final WarpsFeature warpsFeature;
    private final MessageDispatcher messageDispatcher;

    public WarpCommand(WarpsFeature warpsFeature, MessageDispatcher messageDispatcher) {
        this.warpsFeature = warpsFeature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    @CommandCompletion("@warps")
    public void onWarp(Player player, String warpName) {
        if (!warpsFeature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        warpsFeature.warp(player, warpName);
    }

    @Default
    @CommandPermission("jossentials.warp.others")
    @CommandCompletion("@warps @players")
    public void onWarp(CommandSender sender, String warpName, OnlinePlayer target) {
        if (!warpsFeature.isEnabled()) {
            messageDispatcher.send(sender, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        if (!warpsFeature.warp(target.getPlayer(), warpName, sender)) {
            return;
        }
        if (sender == target.getPlayer()) {
            return;
        }
        String message = warpsFeature.plugin().configs().messages().getString("messages.warp-other-sent", "<green>Sent <gold>%player%</gold> to <gold>%warp%</gold>.");
        messageDispatcher.sendWithKey(
            sender,
            "messages.warp-other-sent",
            message.replace("%player%", target.getPlayer().getName()).replace("%warp%", warpName)
        );
    }
}
