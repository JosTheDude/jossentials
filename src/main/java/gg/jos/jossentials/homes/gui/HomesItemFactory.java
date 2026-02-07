package gg.jos.jossentials.homes.gui;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;

import java.util.ArrayList;
import java.util.List;

public final class HomesItemFactory {
    private final Jossentials plugin;
    private final java.util.Map<String, ItemProvider> cache = new java.util.HashMap<>();

    public HomesItemFactory(Jossentials plugin) {
        this.plugin = plugin;
    }

    public ItemProvider create(String path, int slot, String permission) {
        String cacheKey = path + "|" + slot + "|" + permission;
        ItemProvider cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) {
            return ItemProvider.EMPTY;
        }
        String materialName = section.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = section.getString("name", "");
            if (!name.isEmpty()) {
                meta.displayName(ColorUtil.mini(applyPlaceholders(name, slot, permission)));
            }
            List<String> loreLines = section.getStringList("lore");
            if (!loreLines.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreLines) {
                    lore.add(ColorUtil.mini(applyPlaceholders(line, slot, permission)));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        ItemProvider provider = new ItemWrapper(item);
        cache.put(cacheKey, provider);
        return provider;
    }

    private String applyPlaceholders(String input, int slot, String permission) {
        return input.replace("%slot%", String.valueOf(slot))
            .replace("%permission%", permission);
    }

    public void clearCache() {
        cache.clear();
    }
}
