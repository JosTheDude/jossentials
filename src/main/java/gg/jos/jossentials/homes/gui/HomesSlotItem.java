package gg.jos.jossentials.homes.gui;

import gg.jos.jossentials.homes.HomeLocation;
import gg.jos.jossentials.homes.HomesSettings;
import gg.jos.jossentials.homes.teleport.HomesTeleportService;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;

import java.util.Map;

public final class HomesSlotItem extends AbstractItem {
    private final HomesItemFactory itemFactory;
    private final Player player;
    private final int slot;
    private final String permission;
    private final Map<Integer, HomeLocation> homes;
    private final gg.jos.jossentials.util.MessageDispatcher messageDispatcher;
    private final HomesTeleportService teleportService;
    private final HomesSettings settings;

    public HomesSlotItem(HomesItemFactory itemFactory,
                         Player player, int slot, String permission, Map<Integer, HomeLocation> homes,
                         gg.jos.jossentials.util.MessageDispatcher messageDispatcher,
                         HomesTeleportService teleportService,
                         HomesSettings settings) {
        this.itemFactory = itemFactory;
        this.player = player;
        this.slot = slot;
        this.permission = permission;
        this.homes = homes;
        this.messageDispatcher = messageDispatcher;
        this.teleportService = teleportService;
        this.settings = settings;
    }

    @Override
    public ItemProvider getItemProvider(Player viewer) {
        HomeLocation location = homes.get(slot);
        boolean hasPermission = player.hasPermission(permission);
        if (location != null && !hasPermission) {
            return itemFactory.create("homes.gui.items.locked-set", slot, permission);
        }
        if (!hasPermission) {
            return itemFactory.create("homes.gui.items.locked", slot, permission);
        }
        if (location == null) {
            return itemFactory.create("homes.gui.items.empty", slot, permission);
        }
        return itemFactory.create("homes.gui.items.set", slot, permission);
    }

    @Override
    public void handleClick(ClickType clickType, Player clicker, Click click) {
        if (!player.isOnline()) {
            return;
        }
        boolean hasPermission = player.hasPermission(permission);
        HomeLocation existing = homes.get(slot);
        if (!hasPermission) {
            messageDispatcher.send(player, "messages.no-permission", "<red>You do not have permission.");
            return;
        }
        if (existing == null) {
            return;
        }
        if (settings.isTeleportClick(clickType)) {
            var location = existing.toLocation();
            if (location == null) {
                messageDispatcher.send(player, "messages.world-missing", "<red>That world is no longer available.");
                return;
            }
            player.closeInventory();
            teleportService.teleport(player, location, slot);
        }
    }

    public void refresh() {
        notifyWindows();
    }
}
