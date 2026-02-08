package gg.jos.jossentials.warps.feature;

import co.aikar.commands.PaperCommandManager;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.db.Database;
import gg.jos.jossentials.feature.Feature;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.warps.WarpLocation;
import gg.jos.jossentials.warps.WarpsService;
import gg.jos.jossentials.warps.WarpsSettings;
import gg.jos.jossentials.warps.command.DelWarpCommand;
import gg.jos.jossentials.warps.command.SetWarpCommand;
import gg.jos.jossentials.warps.command.WarpCommand;
import gg.jos.jossentials.warps.command.WarpsCommand;
import gg.jos.jossentials.warps.teleport.WarpsTeleportService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.List;

public final class WarpsFeature implements Feature {
    private final Jossentials plugin;
    private final Database database;
    private final PaperCommandManager commandManager;
    private final MessageDispatcher messageDispatcher;

    private WarpsService warpsService;
    private WarpsTeleportService teleportService;
    private WarpsSettings settings;
    private boolean enabled;
    private boolean commandsRegistered;

    public WarpsFeature(Jossentials plugin, Database database, PaperCommandManager commandManager, MessageDispatcher messageDispatcher) {
        this.plugin = plugin;
        this.database = database;
        this.commandManager = commandManager;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public String key() {
        return "warps";
    }

    @Override
    public boolean isEnabledFromConfig() {
        return plugin.configs().features().getBoolean("features.warps.enabled", true);
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
        settings = WarpsSettings.fromConfig(plugin.configs().warps());
        warpsService = new WarpsService(plugin, database);
        teleportService = new WarpsTeleportService(plugin, messageDispatcher, settings);
        if (!commandsRegistered) {
            commandManager.registerCommand(new WarpCommand(this, messageDispatcher));
            commandManager.registerCommand(new WarpsCommand(this, messageDispatcher));
            commandManager.registerCommand(new SetWarpCommand(this, messageDispatcher));
            commandManager.registerCommand(new DelWarpCommand(this, messageDispatcher));
            commandManager.getCommandCompletions().registerCompletion("warps", context -> warpsService.getWarpNames());
            commandsRegistered = true;
        }
        plugin.getServer().getPluginManager().registerEvents(teleportService, plugin);
        warpsService.loadAll();
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
        if (warpsService != null) {
            warpsService.shutdown();
            warpsService = null;
        }
        enabled = false;
    }

    @Override
    public void reload() {
        if (!enabled) {
            return;
        }
        settings = WarpsSettings.fromConfig(plugin.configs().warps());
        if (teleportService != null) {
            teleportService.updateSettings(settings);
        }
    }

    public void warp(Player player, String warpName) {
        if (!enabled || warpsService == null || teleportService == null) {
            String message = plugin.configs().messages().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(player, "messages.feature-disabled", message.replace("%feature%", "warps"));
            return;
        }
        if (!warpsService.isValidName(warpName)) {
            String message = plugin.configs().messages().getString("messages.warp-name-invalid", "<red>That warp name is invalid.");
            messageDispatcher.sendWithKey(player, "messages.warp-name-invalid", message);
            return;
        }
        if (!warpsService.isLoaded()) {
            String message = plugin.configs().messages().getString("messages.warp-loading", "<gray>Loading warps...");
            messageDispatcher.sendWithKey(player, "messages.warp-loading", message);
            return;
        }
        String normalized = warpsService.normalizeName(warpName);
        WarpLocation warp = warpsService.getWarp(normalized);
        if (warp == null) {
            String message = plugin.configs().messages().getString("messages.warp-not-found", "<red>Warp not found.");
            messageDispatcher.sendWithKey(player, "messages.warp-not-found", message.replace("%warp%", normalized));
            return;
        }
        Location destination = warp.toLocation();
        if (destination == null) {
            String message = plugin.configs().messages().getString("messages.world-missing", "<red>That world is no longer available.");
            messageDispatcher.sendWithKey(player, "messages.world-missing", message);
            return;
        }
        teleportService.teleport(player, destination, normalized);
    }

    public void setWarp(Player player, String warpName) {
        if (!enabled || warpsService == null) {
            String message = plugin.configs().messages().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(player, "messages.feature-disabled", message.replace("%feature%", "warps"));
            return;
        }
        if (!warpsService.isValidName(warpName)) {
            String message = plugin.configs().messages().getString("messages.warp-name-invalid", "<red>That warp name is invalid.");
            messageDispatcher.sendWithKey(player, "messages.warp-name-invalid", message);
            return;
        }
        if (!warpsService.isLoaded()) {
            String message = plugin.configs().messages().getString("messages.warp-loading", "<gray>Loading warps...");
            messageDispatcher.sendWithKey(player, "messages.warp-loading", message);
            return;
        }
        String normalized = warpsService.normalizeName(warpName);
        boolean existed = warpsService.getWarp(normalized) != null;
        WarpLocation location = WarpLocation.fromLocation(player.getLocation());
        warpsService.setWarp(normalized, location).whenComplete((success, throwable) -> {
            plugin.scheduler().runEntity(player, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (throwable != null || !Boolean.TRUE.equals(success)) {
                    String message = plugin.configs().messages().getString("messages.warp-set-failed", "<red>Failed to set warp.");
                    messageDispatcher.sendWithKey(player, "messages.warp-set-failed", message.replace("%warp%", normalized));
                    return;
                }
                String messageKey = existed ? "messages.warp-updated" : "messages.warp-set";
                String fallback = existed ? "<green>Warp <gold>%warp%</gold> updated." : "<green>Warp <gold>%warp%</gold> set.";
                String message = plugin.configs().messages().getString(messageKey, fallback);
                messageDispatcher.sendWithKey(player, messageKey, message.replace("%warp%", normalized));
            });
        });
    }

    public void deleteWarp(Player player, String warpName) {
        if (!enabled || warpsService == null) {
            String message = plugin.configs().messages().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(player, "messages.feature-disabled", message.replace("%feature%", "warps"));
            return;
        }
        if (!warpsService.isValidName(warpName)) {
            String message = plugin.configs().messages().getString("messages.warp-name-invalid", "<red>That warp name is invalid.");
            messageDispatcher.sendWithKey(player, "messages.warp-name-invalid", message);
            return;
        }
        String normalized = warpsService.normalizeName(warpName);
        if (!warpsService.isLoaded()) {
            String message = plugin.configs().messages().getString("messages.warp-loading", "<gray>Loading warps...");
            messageDispatcher.sendWithKey(player, "messages.warp-loading", message);
            return;
        }
        if (warpsService.getWarp(normalized) == null) {
            String message = plugin.configs().messages().getString("messages.warp-not-found", "<red>Warp not found.");
            messageDispatcher.sendWithKey(player, "messages.warp-not-found", message.replace("%warp%", normalized));
            return;
        }
        warpsService.deleteWarp(normalized).whenComplete((success, throwable) -> {
            plugin.scheduler().runEntity(player, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (throwable != null || !Boolean.TRUE.equals(success)) {
                    String message = plugin.configs().messages().getString("messages.warp-delete-failed", "<red>Failed to delete warp.");
                    messageDispatcher.sendWithKey(player, "messages.warp-delete-failed", message.replace("%warp%", normalized));
                    return;
                }
                String message = plugin.configs().messages().getString("messages.warp-deleted", "<green>Warp <gold>%warp%</gold> deleted.");
                messageDispatcher.sendWithKey(player, "messages.warp-deleted", message.replace("%warp%", normalized));
            });
        });
    }

    public void listWarps(Player player) {
        if (!enabled || warpsService == null) {
            String message = plugin.configs().messages().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(player, "messages.feature-disabled", message.replace("%feature%", "warps"));
            return;
        }
        if (!warpsService.isLoaded()) {
            String message = plugin.configs().messages().getString("messages.warp-loading", "<gray>Loading warps...");
            messageDispatcher.sendWithKey(player, "messages.warp-loading", message);
            return;
        }
        List<String> names = warpsService.getWarpNames();
        if (names.isEmpty()) {
            String message = plugin.configs().messages().getString("messages.warp-list-empty", "<gray>No warps have been set.");
            messageDispatcher.sendWithKey(player, "messages.warp-list-empty", message);
            return;
        }
        String joined = String.join(", ", names);
        String message = plugin.configs().messages().getString("messages.warp-list", "<gray>Warps: <white>%warps%</white>.");
        messageDispatcher.sendWithKey(player, "messages.warp-list", message.replace("%warps%", joined));
    }
}
