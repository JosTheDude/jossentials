package gg.jos.jossentials.tpa.feature;

import co.aikar.commands.PaperCommandManager;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.feature.Feature;
import gg.jos.jossentials.tpa.TPARequestService;
import gg.jos.jossentials.tpa.TPASettings;
import gg.jos.jossentials.tpa.command.TPAAcceptCommand;
import gg.jos.jossentials.tpa.command.TPACancelCommand;
import gg.jos.jossentials.tpa.command.TPACommand;
import gg.jos.jossentials.tpa.command.TPADenyCommand;
import gg.jos.jossentials.tpa.command.TPAHereCommand;
import gg.jos.jossentials.tpa.teleport.TPATeleportService;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public final class TPAFeature implements Feature {
    private final Jossentials plugin;
    private final PaperCommandManager commandManager;
    private final MessageDispatcher messageDispatcher;

    private TPATeleportService teleportService;
    private TPARequestService requestService;
    private TPASettings settings;
    private boolean enabled;
    private boolean commandsRegistered;

    public TPAFeature(Jossentials plugin, PaperCommandManager commandManager, MessageDispatcher messageDispatcher) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public String key() {
        return "tpa";
    }

    @Override
    public boolean isEnabledFromConfig() {
        return plugin.getConfig().getBoolean("features.tpa.enabled", true);
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
        settings = TPASettings.fromConfig(plugin.getConfig());
        teleportService = new TPATeleportService(plugin, messageDispatcher, settings);
        requestService = new TPARequestService(plugin, messageDispatcher, teleportService, settings);
        if (!commandsRegistered) {
            commandManager.registerCommand(new TPACommand(this, messageDispatcher));
            commandManager.registerCommand(new TPAHereCommand(this, messageDispatcher));
            commandManager.registerCommand(new TPAAcceptCommand(this, messageDispatcher));
            commandManager.registerCommand(new TPADenyCommand(this, messageDispatcher));
            commandManager.registerCommand(new TPACancelCommand(this, messageDispatcher));
            commandsRegistered = true;
        }
        plugin.getServer().getPluginManager().registerEvents(teleportService, plugin);
        plugin.getServer().getPluginManager().registerEvents(requestService, plugin);
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
        if (requestService != null) {
            HandlerList.unregisterAll(requestService);
            requestService.shutdown();
            requestService = null;
        }
        enabled = false;
    }

    @Override
    public void reload() {
        if (!enabled) {
            return;
        }
        settings = TPASettings.fromConfig(plugin.getConfig());
        if (teleportService != null) {
            teleportService.updateSettings(settings);
        }
        if (requestService != null) {
            requestService.updateSettings(settings);
        }
    }

    public void request(Player requester, Player target) {
        if (!enabled || requestService == null) {
            String message = plugin.getConfig().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(requester, "messages.feature-disabled", message.replace("%feature%", "TPA"));
            return;
        }
        requestService.request(requester, target);
    }

    public void requestHere(Player requester, Player target) {
        if (!enabled || requestService == null) {
            String message = plugin.getConfig().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(requester, "messages.feature-disabled", message.replace("%feature%", "TPA"));
            return;
        }
        requestService.requestHere(requester, target);
    }

    public void accept(Player target, Player requester) {
        if (!enabled || requestService == null) {
            String message = plugin.getConfig().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(target, "messages.feature-disabled", message.replace("%feature%", "TPA"));
            return;
        }
        requestService.accept(target, requester);
    }

    public void acceptSingle(Player target) {
        if (!enabled || requestService == null) {
            String message = plugin.getConfig().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(target, "messages.feature-disabled", message.replace("%feature%", "TPA"));
            return;
        }
        requestService.acceptSingle(target);
    }

    public void deny(Player target, Player requester) {
        if (!enabled || requestService == null) {
            String message = plugin.getConfig().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(target, "messages.feature-disabled", message.replace("%feature%", "TPA"));
            return;
        }
        requestService.deny(target, requester);
    }

    public void denySingle(Player target) {
        if (!enabled || requestService == null) {
            String message = plugin.getConfig().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(target, "messages.feature-disabled", message.replace("%feature%", "TPA"));
            return;
        }
        requestService.denySingle(target);
    }

    public void cancel(Player requester) {
        if (!enabled || requestService == null) {
            String message = plugin.getConfig().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(requester, "messages.feature-disabled", message.replace("%feature%", "TPA"));
            return;
        }
        requestService.cancel(requester);
    }
}
