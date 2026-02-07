package gg.jos.jossentials.homes.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import gg.jos.jossentials.homes.feature.HomesFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.entity.Player;

@CommandAlias("homes")
public final class HomesCommand extends BaseCommand {
    private final HomesFeature homesFeature;
    private final MessageDispatcher messageDispatcher;

    public HomesCommand(HomesFeature homesFeature, MessageDispatcher messageDispatcher) {
        this.homesFeature = homesFeature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    public void onHomes(Player player) {
        if (!homesFeature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        homesFeature.open(player);
    }
}
