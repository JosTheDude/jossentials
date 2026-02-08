package gg.jos.jossentials.workbenches;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import java.util.Locale;
import java.util.function.Consumer;

public enum WorkbenchType {
    CRAFTING_TABLE("crafting_table", InventoryType.CRAFTING, player -> player.openWorkbench(null, true)),
    ANVIL("anvil", InventoryType.ANVIL, player -> player.openAnvil(null, true)),
    CARTOGRAPHY_TABLE("cartography_table", InventoryType.CARTOGRAPHY, player -> player.openCartographyTable(null, true)),
    GRINDSTONE("grindstone", InventoryType.GRINDSTONE, player -> player.openGrindstone(null, true)),
    LOOM("loom", InventoryType.LOOM, player -> player.openLoom(null, true)),
    SMITHING_TABLE("smithing_table", InventoryType.SMITHING, player -> player.openSmithingTable(null, true)),
    STONECUTTER("stonecutter", InventoryType.STONECUTTER, player -> player.openStonecutter(null, true)),
    ENCHANTING_TABLE("enchanting_table", InventoryType.ENCHANTING, player -> player.openEnchanting(null, true)),
    BREWING_STAND("brewing_stand", InventoryType.BREWING, player -> player.openInventory(Bukkit.createInventory(player, InventoryType.BREWING))),
    FURNACE("furnace", InventoryType.FURNACE, player -> player.openInventory(Bukkit.createInventory(player, InventoryType.FURNACE))),
    BLAST_FURNACE("blast_furnace", InventoryType.BLAST_FURNACE, player -> player.openInventory(Bukkit.createInventory(player, InventoryType.BLAST_FURNACE))),
    SMOKER("smoker", InventoryType.SMOKER, player -> player.openInventory(Bukkit.createInventory(player, InventoryType.SMOKER)));

    private final String key;
    private final InventoryType inventoryType;
    private final Consumer<Player> opener;

    WorkbenchType(String key, InventoryType inventoryType, Consumer<Player> opener) {
        this.key = key;
        this.inventoryType = inventoryType;
        this.opener = opener;
    }

    public String key() {
        return key;
    }

    public InventoryType inventoryType() {
        return inventoryType;
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
