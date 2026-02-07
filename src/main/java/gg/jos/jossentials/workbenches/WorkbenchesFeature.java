package gg.jos.jossentials.workbenches;

import gg.jos.jossentials.Jossentials;
import co.aikar.commands.PaperCommandManager;
import gg.jos.jossentials.feature.Feature;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.workbenches.command.AnvilCommand;
import gg.jos.jossentials.workbenches.command.BlastFurnaceCommand;
import gg.jos.jossentials.workbenches.command.BrewingStandCommand;
import gg.jos.jossentials.workbenches.command.CartographyTableCommand;
import gg.jos.jossentials.workbenches.command.CraftingTableCommand;
import gg.jos.jossentials.workbenches.command.EnchantingTableCommand;
import gg.jos.jossentials.workbenches.command.FurnaceCommand;
import gg.jos.jossentials.workbenches.command.GrindstoneCommand;
import gg.jos.jossentials.workbenches.command.LoomCommand;
import gg.jos.jossentials.workbenches.command.SmithingTableCommand;
import gg.jos.jossentials.workbenches.command.SmokerCommand;
import gg.jos.jossentials.workbenches.command.StonecutterCommand;

public final class WorkbenchesFeature implements Feature {
    private final Jossentials plugin;
    private final PaperCommandManager commandManager;
    private final MessageDispatcher messageDispatcher;
    private boolean enabled;
    private boolean commandsRegistered;

    public WorkbenchesFeature(Jossentials plugin, PaperCommandManager commandManager, MessageDispatcher messageDispatcher) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public String key() {
        return "workbenches";
    }

    @Override
    public boolean isEnabledFromConfig() {
        return plugin.configs().features().getBoolean("features.workbenches.enabled", true);
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
        if (!commandsRegistered) {
            registerReplacements();
            registerCommands();
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
        if (!enabled) {
            return;
        }
        registerReplacements();
    }

    private void registerReplacements() {
        for (WorkbenchType type : WorkbenchType.values()) {
            String aliasKey = "workbench_" + type.key();
            String path = "workbenches." + type.key();
            boolean enabled = plugin.configs().workbenches().getBoolean(path + ".enabled", true);
            var commands = plugin.configs().workbenches().getStringList(path + ".commands");
            String aliasValue = enabled && commands != null && !commands.isEmpty() ? String.join("|", commands) : "";
            commandManager.getCommandReplacements().addReplacement(aliasKey, aliasValue);
        }
    }

    private void registerCommands() {
        registerIfEnabled(WorkbenchType.CRAFTING_TABLE, () -> commandManager.registerCommand(new CraftingTableCommand(plugin, messageDispatcher)));
        registerIfEnabled(WorkbenchType.ANVIL, () -> commandManager.registerCommand(new AnvilCommand(plugin, messageDispatcher)));
        registerIfEnabled(WorkbenchType.CARTOGRAPHY_TABLE, () -> commandManager.registerCommand(new CartographyTableCommand(plugin, messageDispatcher)));
        registerIfEnabled(WorkbenchType.GRINDSTONE, () -> commandManager.registerCommand(new GrindstoneCommand(plugin, messageDispatcher)));
        registerIfEnabled(WorkbenchType.LOOM, () -> commandManager.registerCommand(new LoomCommand(plugin, messageDispatcher)));
        registerIfEnabled(WorkbenchType.SMITHING_TABLE, () -> commandManager.registerCommand(new SmithingTableCommand(plugin, messageDispatcher)));
        registerIfEnabled(WorkbenchType.STONECUTTER, () -> commandManager.registerCommand(new StonecutterCommand(plugin, messageDispatcher)));
        registerIfEnabled(WorkbenchType.ENCHANTING_TABLE, () -> commandManager.registerCommand(new EnchantingTableCommand(plugin, messageDispatcher)));
        registerIfEnabled(WorkbenchType.BREWING_STAND, () -> commandManager.registerCommand(new BrewingStandCommand(plugin, messageDispatcher)));
        registerIfEnabled(WorkbenchType.FURNACE, () -> commandManager.registerCommand(new FurnaceCommand(plugin, messageDispatcher)));
        registerIfEnabled(WorkbenchType.BLAST_FURNACE, () -> commandManager.registerCommand(new BlastFurnaceCommand(plugin, messageDispatcher)));
        registerIfEnabled(WorkbenchType.SMOKER, () -> commandManager.registerCommand(new SmokerCommand(plugin, messageDispatcher)));
    }

    private void registerIfEnabled(WorkbenchType type, Runnable registrar) {
        String path = "workbenches." + type.key();
        boolean enabled = plugin.configs().workbenches().getBoolean(path + ".enabled", true);
        var commands = plugin.configs().workbenches().getStringList(path + ".commands");
        if (!enabled || commands == null || commands.isEmpty()) {
            return;
        }
        registrar.run();
    }
}
