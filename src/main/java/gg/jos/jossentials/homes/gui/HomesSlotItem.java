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
    private static final String[] LOCKED_PATHS = {"homes.gui.items.locked"};
    private static final String[] LOCKED_SET_PATHS = {"homes.gui.items.locked-set"};
    private static final String[] EMPTY_PATHS = {"homes.gui.items.empty"};
    private static final String[] SET_PATHS = {"homes.gui.items.set"};
    private final HomesItemFactory itemFactory;
    private final Player viewer;
    private final boolean readOnly;
    private final int slot;
    private final String permission;
    private final Map<Integer, HomeLocation> homes;
    private final gg.jos.jossentials.util.MessageDispatcher messageDispatcher;
    private final HomesTeleportService teleportService;
    private final HomesSettings settings;
    private final String pageItemsPath;

    public HomesSlotItem(HomesItemFactory itemFactory,
                         Player viewer, boolean readOnly, int slot, String permission, Map<Integer, HomeLocation> homes,
                         gg.jos.jossentials.util.MessageDispatcher messageDispatcher,
                         HomesTeleportService teleportService,
                         HomesSettings settings,
                         String pageItemsPath) {
        this.itemFactory = itemFactory;
        this.viewer = viewer;
        this.readOnly = readOnly;
        this.slot = slot;
        this.permission = permission;
        this.homes = homes;
        this.messageDispatcher = messageDispatcher;
        this.teleportService = teleportService;
        this.settings = settings;
        this.pageItemsPath = pageItemsPath;
    }

    @Override
    public ItemProvider getItemProvider(Player viewer) {
        HomeLocation location = homes.get(slot);
        boolean hasPermission = readOnly || this.viewer.hasPermission(permission);
        if (location != null && !hasPermission) {
            return itemFactory.create(paths("locked-set", LOCKED_SET_PATHS[0]), slot, permission);
        }
        if (!hasPermission) {
            return itemFactory.create(paths("locked", LOCKED_PATHS[0]), slot, permission);
        }
        if (location == null) {
            return itemFactory.create(paths("empty", EMPTY_PATHS[0]), slot, permission);
        }
        return itemFactory.create(paths("set", SET_PATHS[0]), slot, permission);
    }

    @Override
    public void handleClick(ClickType clickType, Player clicker, Click click) {
        if (!clicker.isOnline()) {
            return;
        }
        boolean hasPermission = readOnly || clicker.hasPermission(permission);
        HomeLocation existing = homes.get(slot);
        if (!hasPermission) {
            messageDispatcher.send(clicker, "messages.no-permission", "<red>You do not have permission.");
            return;
        }
        if (existing == null) {
            return;
        }
        if (settings.isTeleportClick(clickType)) {
            var location = existing.toLocation();
            if (location == null) {
                messageDispatcher.send(clicker, "messages.world-missing", "<red>That world is no longer available.");
                return;
            }
            clicker.closeInventory();
            teleportService.teleport(clicker, location, slot);
        }
    }

    public void refresh() {
        notifyWindows();
    }

    private String[] paths(String key, String fallback) {
        if (pageItemsPath == null || pageItemsPath.isBlank()) {
            return new String[]{fallback};
        }
        return new String[]{pageItemsPath + "." + key, fallback};
    }
}
