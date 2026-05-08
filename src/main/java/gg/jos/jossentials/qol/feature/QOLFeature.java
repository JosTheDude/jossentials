package gg.jos.jossentials.qol.feature;

import co.aikar.commands.PaperCommandManager;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.feature.Feature;
import gg.jos.jossentials.qol.QOLSettings;
import gg.jos.jossentials.qol.BackService;
import gg.jos.jossentials.qol.command.BackCommand;
import gg.jos.jossentials.qol.command.EnderChestCommand;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.util.TeleportUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class QOLFeature implements Feature {
    private final Jossentials plugin;
    private final PaperCommandManager commandManager;
    private final MessageDispatcher messageDispatcher;

    private QOLSettings settings;
    private BackService backService;

    private boolean enabled;
    private boolean commandsRegistered;

    public QOLFeature(Jossentials plugin, PaperCommandManager commandManager, MessageDispatcher messageDispatcher) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public String key() {
        return "qol";
    }

    @Override
    public boolean isEnabledFromConfig() {
        return plugin.configs().features().getBoolean("features.qol.enabled", true);
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

        settings = QOLSettings.fromConfig(plugin.configs().qol());
        if (!commandsRegistered) {
            commandManager.registerCommand(new EnderChestCommand(this, messageDispatcher));
            commandManager.registerCommand(new BackCommand(this, messageDispatcher));
            commandsRegistered = true;
        }
        syncBackService();

        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }
        shutdownBackService();
        enabled = false;
    }

    @Override
    public void reload() {
        if (!enabled) {
            return;
        }
        settings = QOLSettings.fromConfig(plugin.configs().qol());
        syncBackService();
    }

    public boolean isEnderChestCommandEnabled() {
        return settings != null && settings.enderchestCommandEnabled;
    }

    public boolean isBackCommandEnabled() {
        return settings != null && settings.backCommandEnabled;
    }

    public void teleportBack(Player player) {
        if (backService == null) {
            String message = plugin.configs().messages().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(player, "messages.feature-disabled", message.replace("%feature%", "back"));
            return;
        }
        backService.teleportBack(player);
    }

    private void syncBackService() {
        if (!settings.backCommandEnabled) {
            shutdownBackService();
            return;
        }
        if (backService == null) {
            backService = new BackService(plugin, messageDispatcher, settings.backSettings);
            plugin.getServer().getPluginManager().registerEvents(backService, plugin);
        } else {
            backService.updateSettings(settings.backSettings);
        }
        TeleportUtil.setBackLocationRecorder(backService::recordTeleportOrigin);
    }

    private void shutdownBackService() {
        if (backService == null) {
            return;
        }
        TeleportUtil.setBackLocationRecorder(null);
        HandlerList.unregisterAll(backService);
        backService.shutdown();
        backService = null;
    }
}
