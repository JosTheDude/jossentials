package gg.jos.jossentials;

import co.aikar.commands.PaperCommandManager;
import gg.jos.jossentials.db.Database;
import gg.jos.jossentials.command.ReloadCommand;
import gg.jos.jossentials.feature.FeatureManager;
import gg.jos.jossentials.homes.feature.HomesFeature;
import gg.jos.jossentials.tpa.feature.TPAFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Jossentials extends JavaPlugin {
    private Database database;
    private MessageDispatcher messageDispatcher;
    private FeatureManager featureManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            database = new Database(this);
            database.initialize();
        } catch (Exception ex) {
            getLogger().severe("Failed to initialize database: " + ex.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        messageDispatcher = new MessageDispatcher(this);
        PaperCommandManager commandManager = new PaperCommandManager(this);
        featureManager = new FeatureManager();
        featureManager.register(new HomesFeature(this, database, commandManager, messageDispatcher));
        featureManager.register(new TPAFeature(this, commandManager, messageDispatcher));
        featureManager.enableConfigured();
        commandManager.registerCommand(new ReloadCommand(this, featureManager, messageDispatcher));

    }

    @Override
    public void onDisable() {
        if (featureManager != null) {
            featureManager.disableAll();
        }
        if (database != null) {
            database.shutdown();
        }
    }
}
