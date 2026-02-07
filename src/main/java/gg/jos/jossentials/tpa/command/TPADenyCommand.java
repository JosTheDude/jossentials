package gg.jos.jossentials.tpa.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import gg.jos.jossentials.tpa.feature.TPAFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.entity.Player;

@CommandAlias("tpdeny|tpno")
@CommandPermission("jossentials.tpa.deny")
public final class TPADenyCommand extends BaseCommand {
    private final TPAFeature tpaFeature;
    private final MessageDispatcher messageDispatcher;

    public TPADenyCommand(TPAFeature tpaFeature, MessageDispatcher messageDispatcher) {
        this.tpaFeature = tpaFeature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    public void onDeny(Player player, @Optional Player requester) {
        if (!tpaFeature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        if (requester == null) {
            tpaFeature.denySingle(player);
        } else {
            tpaFeature.deny(player, requester);
        }
    }
}
