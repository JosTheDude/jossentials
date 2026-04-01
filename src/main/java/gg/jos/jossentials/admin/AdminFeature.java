package gg.jos.jossentials.admin;

import co.aikar.commands.PaperCommandManager;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.admin.command.*;
import gg.jos.jossentials.db.Database;
import gg.jos.jossentials.feature.Feature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AdminFeature implements Feature, Listener {
    private final Jossentials plugin;
    private final Database database;
    private final PaperCommandManager commandManager;
    private final MessageDispatcher messageDispatcher;

    private AdminSettings settings;
    private SeenService seenService;
    private boolean enabled;
    private boolean commandsRegistered;

    public AdminFeature(Jossentials plugin, Database database, PaperCommandManager commandManager, MessageDispatcher messageDispatcher) {
        this.plugin = plugin;
        this.database = database;
        this.commandManager = commandManager;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public String key() {
        return "admin";
    }

    @Override
    public boolean isEnabledFromConfig() {
        return plugin.configs().features().getBoolean("features.admin.enabled", true);
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
        settings = AdminSettings.fromConfig(plugin.configs().admin());
        seenService = new SeenService(plugin, database);
        seenService.loadAll();
        applyReplacements();
        if (!commandsRegistered) {
            commandManager.registerCommand(new TPCommand(this, messageDispatcher));
            commandManager.registerCommand(new TPPosCommand(this, messageDispatcher));
            commandManager.registerCommand(new NearCommand(this, messageDispatcher));
            commandManager.registerCommand(new SpeedCommand(this, messageDispatcher));
            commandManager.registerCommand(new SeenCommand(this, messageDispatcher));
            commandManager.registerCommand(new GamemodeCommand(this, messageDispatcher));
            commandManager.getCommandCompletions().registerCompletion("seenplayers", context -> seenPlayerNames());
            commandManager.getCommandCompletions().registerCompletion("adminspeedtypes", context -> List.of("walk", "fly"));
            commandManager.getCommandCompletions().registerCompletion("adminspeedvalues", context -> speedValues());
            commandsRegistered = true;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            recordLogin(player);
        }
        enabled = true;
    }

    @Override
    public void disable() {
        if (seenService != null) {
            seenService.shutdown();
            seenService = null;
        }
        HandlerList.unregisterAll(this);
        enabled = false;
    }

    @Override
    public void reload() {
        settings = AdminSettings.fromConfig(plugin.configs().admin());
        applyReplacements();
    }

    public AdminSettings settings() {
        return settings;
    }

    public Jossentials plugin() {
        return plugin;
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

    public List<String> seenPlayerNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        if (seenService != null) {
            names.addAll(seenService.names());
        }
        return new ArrayList<>(names);
    }

    public SeenService seenService() {
        return seenService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        recordLogin(event.getPlayer());
    }

    private void applyReplacements() {
        if (settings == null) {
            return;
        }
        commandManager.getCommandReplacements().addReplacements(
            "admin-tp-aliases", settings.tpAliasesReplacement(),
            "admin-tppos-aliases", settings.tpPosAliasesReplacement(),
            "admin-near-aliases", settings.nearAliasesReplacement(),
            "admin-speed-aliases", settings.speedAliasesReplacement(),
            "admin-seen-aliases", settings.seenAliasesReplacement(),
            "admin-gamemode-aliases", settings.gamemodeAliasesReplacement()
        );
    }

    private void recordLogin(Player player) {
        if (seenService == null) {
            return;
        }
        seenService.recordLogin(player.getUniqueId(), player.getName(), System.currentTimeMillis());
    }

    private List<String> speedValues() {
        List<String> values = new ArrayList<>();
        for (int i = 0; i <= settings.maxSpeed(); i++) {
            values.add(String.valueOf(i));
        }
        return values;
    }
}
