package gg.jos.jossentials.spawn;

import co.aikar.commands.PaperCommandManager;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.feature.Feature;
import gg.jos.jossentials.spawn.command.SetSpawnCommand;
import gg.jos.jossentials.spawn.command.SpawnCommand;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public final class SpawnFeature implements Feature {
    private final Jossentials plugin;
    private final PaperCommandManager commandManager;
    private final MessageDispatcher messageDispatcher;

    private SpawnService spawnService;
    private SpawnTeleportService teleportService;
    private SpawnSettings settings;
    private boolean enabled;
    private boolean commandsRegistered;

    public SpawnFeature(Jossentials plugin, PaperCommandManager commandManager, MessageDispatcher messageDispatcher) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public String key() {
        return "spawn";
    }

    @Override
    public boolean isEnabledFromConfig() {
        return plugin.configs().features().getBoolean("features.spawn.enabled", true);
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
        settings = SpawnSettings.fromConfig(plugin.configs().spawn());
        spawnService = new SpawnService(plugin);
        teleportService = new SpawnTeleportService(plugin, messageDispatcher, settings);
        if (!commandsRegistered) {
            commandManager.registerCommand(new SpawnCommand(this, messageDispatcher));
            commandManager.registerCommand(new SetSpawnCommand(this, messageDispatcher));
            commandsRegistered = true;
        }
        plugin.getServer().getPluginManager().registerEvents(teleportService, plugin);
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }
        if (teleportService != null) {
            HandlerList.unregisterAll(teleportService);
            teleportService.shutdown();
            teleportService = null;
        }
        spawnService = null;
        enabled = false;
    }

    @Override
    public void reload() {
        if (!enabled) {
            return;
        }
        settings = SpawnSettings.fromConfig(plugin.configs().spawn());
        if (teleportService != null) {
            teleportService.updateSettings(settings);
        }
    }

    public void teleport(Player player) {
        if (!enabled || teleportService == null || spawnService == null) {
            String message = plugin.configs().messages().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(player, "messages.feature-disabled", message.replace("%feature%", "spawn"));
            return;
        }
        var destination = spawnService.getSpawnLocation(player.getWorld());
        if (destination == null) {
            String message = plugin.configs().messages().getString("messages.spawn-not-set", "<red>Spawn has not been set yet.");
            messageDispatcher.sendWithKey(player, "messages.spawn-not-set", message);
            return;
        }
        teleportService.teleport(player, destination);
    }

    public void setSpawn(Player player) {
        if (!enabled || spawnService == null) {
            String message = plugin.configs().messages().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(player, "messages.feature-disabled", message.replace("%feature%", "spawn"));
            return;
        }
        spawnService.setSpawn(player.getLocation());
        String message = plugin.configs().messages().getString("messages.spawn-set", "<green>Spawn set.");
        messageDispatcher.sendWithKey(player, "messages.spawn-set", message);
    }
}
