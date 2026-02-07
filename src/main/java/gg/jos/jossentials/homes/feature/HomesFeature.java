package gg.jos.jossentials.homes.feature;

import co.aikar.commands.PaperCommandManager;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.db.Database;
import gg.jos.jossentials.feature.Feature;
import gg.jos.jossentials.homes.HomesService;
import gg.jos.jossentials.homes.HomesSettings;
import gg.jos.jossentials.homes.command.HomesCommand;
import gg.jos.jossentials.homes.gui.HomesGui;
import gg.jos.jossentials.homes.teleport.HomesTeleportService;
import gg.jos.jossentials.homes.util.DeleteConfirmationManager;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public final class HomesFeature implements Feature {
    private final Jossentials plugin;
    private final Database database;
    private final PaperCommandManager commandManager;
    private final MessageDispatcher messageDispatcher;

    private HomesService homesService;
    private HomesGui homesGui;
    private HomesTeleportService teleportService;
    private DeleteConfirmationManager deleteConfirmationManager;
    private HomesSettings settings;
    private boolean enabled;
    private boolean commandRegistered;

    public HomesFeature(Jossentials plugin, Database database, PaperCommandManager commandManager, MessageDispatcher messageDispatcher) {
        this.plugin = plugin;
        this.database = database;
        this.commandManager = commandManager;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public String key() {
        return "homes";
    }

    @Override
    public boolean isEnabledFromConfig() {
        return plugin.configs().features().getBoolean("features.homes.enabled", true);
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
        settings = HomesSettings.fromConfig(plugin.configs().homes());
        homesService = new HomesService(plugin, database);
        deleteConfirmationManager = new DeleteConfirmationManager();
        teleportService = new HomesTeleportService(plugin, messageDispatcher, settings);
        homesGui = new HomesGui(plugin, homesService, messageDispatcher, deleteConfirmationManager, teleportService, settings);
        if (!commandRegistered) {
            commandManager.registerCommand(new HomesCommand(this, messageDispatcher));
            commandRegistered = true;
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
            teleportService = null;
        }
        if (homesService != null) {
            homesService.shutdown();
            homesService = null;
        }
        homesGui = null;
        deleteConfirmationManager = null;
        enabled = false;
    }

    @Override
    public void reload() {
        if (!enabled) {
            return;
        }
        settings = HomesSettings.fromConfig(plugin.configs().homes());
        if (teleportService != null) {
            teleportService.updateSettings(settings);
        }
        if (homesGui != null) {
            homesGui.reload(settings);
        }
    }

    public void open(Player player) {
        if (!enabled || homesGui == null) {
            String message = plugin.configs().messages().getString("messages.feature-disabled", "<red>This feature is disabled.");
            messageDispatcher.sendWithKey(player, "messages.feature-disabled", message.replace("%feature%", "homes"));
            return;
        }
        homesGui.open(player);
    }
}
