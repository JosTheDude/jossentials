package gg.jos.jossentials.fly;

import co.aikar.commands.PaperCommandManager;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.feature.Feature;
import gg.jos.jossentials.fly.command.FlyCommand;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public final class FlyFeature implements Feature {
    private final Jossentials plugin;
    private final PaperCommandManager commandManager;
    private final MessageDispatcher messageDispatcher;

    private FlySettings settings;
    private boolean enabled;
    private boolean commandsRegistered;

    public FlyFeature(Jossentials plugin, PaperCommandManager commandManager, MessageDispatcher messageDispatcher) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public String key() {
        return "fly";
    }

    @Override
    public boolean isEnabledFromConfig() {
        return plugin.configs().features().getBoolean("features.fly.enabled", true);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        if (enabled) {
            return;
        }

        settings = FlySettings.fromConfig(plugin.configs().fly());
        applyReplacements();
        if (!commandsRegistered) {
            commandManager.registerCommand(new FlyCommand(this, messageDispatcher));
            commandsRegistered = true;
        }
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
    }

    @Override
    public void reload() {
        settings = FlySettings.fromConfig(plugin.configs().fly());
        applyReplacements();
    }

    public Jossentials plugin() {
        return plugin;
    }

    public FlySettings settings() {
        return settings;
    }

    public Player findOnlinePlayer(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(input)) {
                return online;
            }
        }
        return null;
    }

    public boolean canNaturallyFly(Player player) {
        GameMode gameMode = player.getGameMode();
        return gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR;
    }

    public void setManualFlight(Player player, boolean enabled) {
        if (canNaturallyFly(player)) {
            player.setFlying(enabled);
            return;
        }

        player.setAllowFlight(enabled);
        player.setFlying(enabled);
        if (!enabled) {
            player.setFallDistance(0.0F);
        }
    }

    private void applyReplacements() {
        if (settings == null) {
            return;
        }
        commandManager.getCommandReplacements().addReplacement("fly-aliases", settings.aliasesReplacement());
    }
}
