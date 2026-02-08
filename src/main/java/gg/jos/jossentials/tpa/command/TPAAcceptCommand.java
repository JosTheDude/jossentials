package gg.jos.jossentials.tpa.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import gg.jos.jossentials.tpa.feature.TPAFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("tpaccept|tpyes")
@CommandPermission("jossentials.tpa.accept")
public final class TPAAcceptCommand extends BaseCommand {
    private final TPAFeature tpaFeature;
    private final MessageDispatcher messageDispatcher;

    public TPAAcceptCommand(TPAFeature tpaFeature, MessageDispatcher messageDispatcher) {
        this.tpaFeature = tpaFeature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    public void onAccept(Player player, @Optional String requesterName) {
        if (!tpaFeature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        if (requesterName == null || requesterName.isBlank()) {
            tpaFeature.acceptSingle(player);
        } else {
            Player requester = Bukkit.getPlayerExact(requesterName);
            if (requester == null) {
                messageDispatcher.send(player, "messages.tpa-no-pending", "<red>You have no pending teleport requests.");
                return;
            }
            tpaFeature.accept(player, requester);
        }
    }
}
