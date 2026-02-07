package gg.jos.jossentials.tpa.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import gg.jos.jossentials.tpa.feature.TPAFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.entity.Player;

@CommandAlias("tpa|tpask")
@CommandPermission("jossentials.tpa")
public final class TPACommand extends BaseCommand {
    private final TPAFeature tpaFeature;
    private final MessageDispatcher messageDispatcher;

    public TPACommand(TPAFeature tpaFeature, MessageDispatcher messageDispatcher) {
        this.tpaFeature = tpaFeature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    public void onTpa(Player player, Player target) {
        if (!tpaFeature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        tpaFeature.request(player, target);
    }
}
