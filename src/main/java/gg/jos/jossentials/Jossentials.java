package gg.jos.jossentials;

import co.aikar.commands.PaperCommandManager;
import gg.jos.jossentials.db.Database;
import gg.jos.jossentials.command.ReloadCommand;
import gg.jos.jossentials.config.ConfigManager;
import gg.jos.jossentials.feature.FeatureManager;
import gg.jos.jossentials.homes.feature.HomesFeature;
import gg.jos.jossentials.tpa.feature.TPAFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.util.SchedulerAdapter;
import gg.jos.jossentials.workbenches.WorkbenchesFeature;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Jossentials extends JavaPlugin {
    private ConfigManager configs;
    private SchedulerAdapter scheduler;
    private Database database;
    private MessageDispatcher messageDispatcher;
    private FeatureManager featureManager;

    @Override
    public void onEnable() {
        configs = new ConfigManager(this);
        configs.load();
        scheduler = new SchedulerAdapter(this);

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
        featureManager.register(new WorkbenchesFeature(this, messageDispatcher));
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

    public ConfigManager configs() {
        return configs;
    }

    public SchedulerAdapter scheduler() {
        return scheduler;
    }
}
