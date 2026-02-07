package gg.jos.jossentials.tpa.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import gg.jos.jossentials.tpa.feature.TPAFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.entity.Player;

@CommandAlias("tpahere")
@CommandPermission("jossentials.tpa.here")
public final class TPAHereCommand extends BaseCommand {
    private final TPAFeature tpaFeature;
    private final MessageDispatcher messageDispatcher;

    public TPAHereCommand(TPAFeature tpaFeature, MessageDispatcher messageDispatcher) {
        this.tpaFeature = tpaFeature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    public void onTpaHere(Player player, Player target) {
        if (!tpaFeature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        tpaFeature.requestHere(player, target);
    }
}
