package gg.jos.jossentials.tpa.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import gg.jos.jossentials.tpa.feature.TPAFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.entity.Player;

@CommandAlias("tptoggle")
@CommandPermission("jossentials.tpa.toggle")
public final class TPToggleCommand extends BaseCommand {
    private final TPAFeature tpaFeature;
    private final MessageDispatcher messageDispatcher;

    public TPToggleCommand(TPAFeature tpaFeature, MessageDispatcher messageDispatcher) {
        this.tpaFeature = tpaFeature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    public void onToggle(Player player) {
        if (!tpaFeature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        boolean enabled = tpaFeature.toggleTpa(player);
        String messageKey = enabled ? "messages.tpa-toggle-on" : "messages.tpa-toggle-off";
        String fallback = enabled ? "<green>Teleport requests enabled." : "<red>Teleport requests disabled.";
        messageDispatcher.send(player, messageKey, fallback);
    }
}
