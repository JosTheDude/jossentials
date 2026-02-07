package gg.jos.jossentials.workbenches;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import java.util.Locale;
import java.util.function.Consumer;

public enum WorkbenchType {
    CRAFTING_TABLE("crafting_table", player -> player.openWorkbench(null, true)),
    ANVIL("anvil", player -> player.openAnvil(null, true)),
    CARTOGRAPHY_TABLE("cartography_table", player -> player.openCartographyTable(null, true)),
    GRINDSTONE("grindstone", player -> player.openGrindstone(null, true)),
    LOOM("loom", player -> player.openLoom(null, true)),
    SMITHING_TABLE("smithing_table", player -> player.openSmithingTable(null, true)),
    STONECUTTER("stonecutter", player -> player.openStonecutter(null, true)),
    ENCHANTING_TABLE("enchanting_table", player -> player.openEnchanting(null, true)),
    BREWING_STAND("brewing_stand", player -> player.openInventory(Bukkit.createInventory(player, InventoryType.BREWING))),
    FURNACE("furnace", player -> player.openInventory(Bukkit.createInventory(player, InventoryType.FURNACE))),
    BLAST_FURNACE("blast_furnace", player -> player.openInventory(Bukkit.createInventory(player, InventoryType.BLAST_FURNACE))),
    SMOKER("smoker", player -> player.openInventory(Bukkit.createInventory(player, InventoryType.SMOKER)));

    private final String key;
    private final Consumer<Player> opener;

    WorkbenchType(String key, Consumer<Player> opener) {
        this.key = key;
        this.opener = opener;
    }

    public String key() {
        return key;
    }

    public void open(Player player) {
        opener.accept(player);
    }

    public static WorkbenchType fromKey(String key) {
        if (key == null) {
            return null;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        for (WorkbenchType type : values()) {
            if (type.key.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
