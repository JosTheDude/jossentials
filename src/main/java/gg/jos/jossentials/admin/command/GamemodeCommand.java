package gg.jos.jossentials.admin.command;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Subcommand;
import gg.jos.jossentials.admin.AdminFeature;
import gg.jos.jossentials.command.helper.JossCommand;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("%{admin-gamemode-aliases}")
public class GamemodeCommand extends JossCommand<AdminFeature> {

    public GamemodeCommand(AdminFeature feature, MessageDispatcher messageDispatcher) {
        super(feature, messageDispatcher);
    }

    @Subcommand("creative")
    @CommandAlias("gmc")
    public void creative(CommandSender sender) {
        if (!featureEnabled(sender)) return;
        if (!playerOnly(sender)) return;
        if (!permission(sender, "jossentials.gamemode.creative")) return;
        Player player = (Player) sender;

        player.setGameMode(GameMode.CREATIVE);
        messageDispatcher.send(player, "messages.admin-gamemode-change.creative", "<#a6e3a1>Game mode set to Creative.");
    }

    @Subcommand("survival")
    @CommandAlias("gms")
    public void survival(CommandSender sender) {
        if (!featureEnabled(sender)) return;
        if (!playerOnly(sender)) return;
        if (!permission(sender, "jossentials.gamemode.survival")) return;
        Player player = (Player) sender;

        player.setGameMode(GameMode.SURVIVAL);
        messageDispatcher.send(player, "messages.admin-gamemode-change.survival", "<#a6e3a1>Game mode set to Survival.");
    }

    @Subcommand("adventure")
    @CommandAlias("gma")
    public void adventure(CommandSender sender) {
        if (!featureEnabled(sender)) return;
        if (!playerOnly(sender)) return;
        if (!permission(sender, "jossentials.gamemode.adventure")) return;
        Player player = (Player) sender;

        player.setGameMode(GameMode.ADVENTURE);
        messageDispatcher.send(player, "messages.admin-gamemode-change.adventure", "<#a6e3a1>Game mode set to Adventure.");
    }

    @Subcommand("spectator")
    @CommandAlias("gmsp")
    public void spectator(CommandSender sender) {
        if (!featureEnabled(sender)) return;
        if (!playerOnly(sender)) return;
        if (!permission(sender, "jossentials.gamemode.spectator")) return;
        Player player = (Player) sender;

        player.setGameMode(GameMode.SPECTATOR);
        messageDispatcher.send(player, "messages.admin-gamemode-change.spectator", "<#a6e3a1>Game mode set to Spectator.");
    }

}
