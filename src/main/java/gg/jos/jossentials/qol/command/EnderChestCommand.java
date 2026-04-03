package gg.jos.jossentials.qol.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import gg.jos.jossentials.command.helper.JossCommand;
import gg.jos.jossentials.qol.feature.QOLFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("enderchest|ec|echest")
public class EnderChestCommand extends JossCommand<QOLFeature> {

    public EnderChestCommand(QOLFeature feature, MessageDispatcher messageDispatcher) {
        super(feature, messageDispatcher);
    }

    @Default
    public void def(CommandSender sender) {
        if (!featureEnabled(sender)) return;
        if (!playerOnly(sender)) return;
        if (!permission(sender, "jossentials.enderchest")) return;

        Player player = (Player) sender;
        player.openInventory(player.getEnderChest());
    }

}
