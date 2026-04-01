package gg.jos.jossentials.qol.feature;

import co.aikar.commands.PaperCommandManager;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.qol.command.EnderChestCommand;
import gg.jos.jossentials.feature.Feature;
import gg.jos.jossentials.qol.QOLSettings;
import gg.jos.jossentials.util.MessageDispatcher;

public class QOLFeature implements Feature {
    private final Jossentials plugin;
    private final PaperCommandManager commandManager;
    private final MessageDispatcher messageDispatcher;

    private QOLSettings settings;

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
        if (enabled) return;

        settings = QOLSettings.fromConfig(plugin.configs().qol());
        if (!commandsRegistered) {
            if (settings.enderchestCommandEnabled)
                commandManager.registerCommand(new EnderChestCommand(this, messageDispatcher));

            commandsRegistered = true;
        }

        enabled = true;
    }

    @Override
    public void disable() {

    }

    @Override
    public void reload() {

    }
}
