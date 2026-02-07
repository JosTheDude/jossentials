package gg.jos.jossentials.workbenches;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.feature.Feature;
import gg.jos.jossentials.util.MessageDispatcher;

public final class WorkbenchesFeature implements Feature {
    private final Jossentials plugin;
    private final WorkbenchCommandRegistry registry;
    private boolean enabled;

    public WorkbenchesFeature(Jossentials plugin, MessageDispatcher messageDispatcher) {
        this.plugin = plugin;
        this.registry = new WorkbenchCommandRegistry(plugin, messageDispatcher);
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
        registry.registerConfigured();
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }
        registry.clear();
        enabled = false;
    }

    @Override
    public void reload() {
        if (!enabled) {
            return;
        }
        registry.clear();
        registry.registerConfigured();
    }
}
