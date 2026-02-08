package gg.jos.jossentials.workbenches;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.workbenches.command.WorkbenchCommandBase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WorkbenchInventoryListener implements Listener {
    private final Jossentials plugin;

    public WorkbenchInventoryListener(Jossentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        List<MetadataValue> metadata = player.getMetadata(WorkbenchCommandBase.METADATA_KEY);
        if (metadata == null || metadata.isEmpty()) {
            return;
        }

        Optional<MetadataValue> owned = metadata.stream()
            .filter(value -> value.getOwningPlugin() == plugin)
            .findFirst();
        if (owned.isEmpty()) {
            return;
        }

        String typeKey = owned.get().asString();
        WorkbenchType type = WorkbenchType.fromKey(typeKey);
        player.removeMetadata(WorkbenchCommandBase.METADATA_KEY, plugin);
        if (type == null || event.getInventory().getType() != type.inventoryType()) {
            return;
        }

        Inventory inventory = event.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
            }
        }
        inventory.clear();
    }
}
