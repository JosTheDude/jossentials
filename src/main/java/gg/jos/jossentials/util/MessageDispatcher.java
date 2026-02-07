package gg.jos.jossentials.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import gg.jos.jossentials.Jossentials;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MessageDispatcher {
    private final Jossentials plugin;
    private volatile DeliverySettings settings;

    public MessageDispatcher(Jossentials plugin) {
        this.plugin = plugin;
        reload();
    }

    public void send(Player player, String messageKey, String fallbackMessage) {
        String message = plugin.configs().messages().getString(messageKey, fallbackMessage);
        sendWithKey(player, messageKey, message);
    }

    public void sendWithKey(Player player, String messageKey, String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return;
        }
        DeliverySettings current = settings;
        String type = resolveType(current, messageKey);
        Component component = ColorUtil.mini(rawMessage);
        switch (type.toLowerCase(Locale.ROOT)) {
            case "actionbar" -> player.sendActionBar(component);
            case "title" -> sendTitle(player, component, current);
            default -> player.sendMessage(component);
        }
        playSound(player, current, messageKey);
    }

    private void sendTitle(Player player, Component title, DeliverySettings current) {
        player.showTitle(Title.title(title, current.titleSubtitle, current.titleTimes));
    }

    private void playSound(Player player, DeliverySettings current, String messageKey) {
        SoundSettings soundSettings = current.soundSettings;
        if (!soundSettings.enabled) {
            return;
        }
        SoundEntry entry = resolveSound(soundSettings, messageKey);
        if (entry == null || !entry.enabled || entry.sound == null) {
            return;
        }
        player.playSound(player.getLocation(), entry.sound, entry.volume, entry.pitch);
    }

    public void reload() {
        ConfigurationSection section = plugin.configs().messages().getConfigurationSection("messages.delivery");
        String type = section != null ? section.getString("type", "message") : "message";

        String subtitleRaw = section != null ? section.getString("title.subtitle", "") : "";
        Component subtitle = subtitleRaw == null || subtitleRaw.isEmpty()
            ? Component.empty()
            : ColorUtil.mini(subtitleRaw);
        int fadeIn = section != null ? section.getInt("title.fade-in-ticks", 10) : 10;
        int stay = section != null ? section.getInt("title.stay-ticks", 40) : 40;
        int fadeOut = section != null ? section.getInt("title.fade-out-ticks", 10) : 10;
        Title.Times times = Title.Times.times(
            Duration.ofMillis(fadeIn * 50L),
            Duration.ofMillis(stay * 50L),
            Duration.ofMillis(fadeOut * 50L)
        );

        Map<String, String> overrides = new HashMap<>();
        if (section != null) {
            ConfigurationSection overrideSection = section.getConfigurationSection("overrides");
            if (overrideSection != null) {
                for (String key : overrideSection.getKeys(false)) {
                    String value = overrideSection.getString(key, "");
                    if (value != null && !value.isEmpty()) {
                        overrides.put(key.toLowerCase(Locale.ROOT), value);
                    }
                }
            }
        }

        SoundSettings soundSettings = loadSoundSettings(plugin.configs().messages().getConfigurationSection("messages.sounds"));

        settings = new DeliverySettings(type, subtitle, times, overrides, soundSettings);
    }

    private SoundSettings loadSoundSettings(ConfigurationSection section) {
        if (section == null) {
            return new SoundSettings(false, null, new HashMap<>());
        }
        boolean enabled = section.getBoolean("enabled", true);
        SoundEntry defaultEntry = loadSoundEntry(section.getConfigurationSection("default"));
        Map<String, SoundEntry> overrides = new HashMap<>();
        ConfigurationSection overrideSection = section.getConfigurationSection("overrides");
        if (overrideSection != null) {
            for (String key : overrideSection.getKeys(false)) {
                SoundEntry entry = loadSoundEntry(overrideSection.getConfigurationSection(key));
                if (entry != null) {
                    overrides.put(key.toLowerCase(Locale.ROOT), entry);
                }
            }
        }
        return new SoundSettings(enabled, defaultEntry, overrides);
    }

    private SoundEntry loadSoundEntry(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String name = section.getString("name", "");
        if (name == null || name.isEmpty() || "none".equalsIgnoreCase(name)) {
            return new SoundEntry(null, 1.0f, 1.0f, false);
        }
        Sound sound;
        try {
            sound = Sound.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
        float volume = (float) section.getDouble("volume", 1.0);
        float pitch = (float) section.getDouble("pitch", 1.0);
        return new SoundEntry(sound, volume, pitch, true);
    }

    private SoundEntry resolveSound(SoundSettings settings, String messageKey) {
        if (messageKey == null) {
            return settings.defaultEntry;
        }
        String normalized = normalizeKey(messageKey);
        SoundEntry override = settings.overrides.get(normalized);
        if (override != null) {
            return override;
        }
        return settings.defaultEntry;
    }

    private record DeliverySettings(
        String type,
        Component titleSubtitle,
        Title.Times titleTimes,
        Map<String, String> overrides,
        SoundSettings soundSettings
    ) {
    }

    private record SoundSettings(
        boolean enabled,
        SoundEntry defaultEntry,
        Map<String, SoundEntry> overrides
    ) {
    }

    private record SoundEntry(
        Sound sound,
        float volume,
        float pitch,
        boolean enabled
    ) {
    }

    private String resolveType(DeliverySettings current, String messageKey) {
        if (messageKey == null) {
            return normalizeType(current.type);
        }
        String normalized = normalizeKey(messageKey);
        String override = current.overrides.get(normalized);
        if (override != null && !override.isEmpty()) {
            if ("default".equalsIgnoreCase(override)) {
                return normalizeType(current.type);
            }
            return normalizeType(override);
        }
        return normalizeType(current.type);
    }

    private String normalizeType(String value) {
        if (value == null) {
            return "message";
        }
        if ("bossbar".equalsIgnoreCase(value)) {
            return "message";
        }
        return value;
    }

    private String normalizeKey(String messageKey) {
        String key = messageKey;
        if (key.startsWith("messages.")) {
            key = key.substring("messages.".length());
        }
        return key.toLowerCase(Locale.ROOT);
    }
}
