package gg.jos.jossentials.homes.gui;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.homes.HomeLocation;
import gg.jos.jossentials.homes.HomesService;
import gg.jos.jossentials.homes.HomesSettings;
import gg.jos.jossentials.homes.teleport.HomesTeleportService;
import gg.jos.jossentials.homes.util.DeleteConfirmationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;

import java.util.Map;

public final class HomesSlotItem extends AbstractItem {
    private final Jossentials plugin;
    private final HomesService homesService;
    private final HomesItemFactory itemFactory;
    private final Player player;
    private final int slot;
    private final String permission;
    private final Map<Integer, HomeLocation> homes;
    private final gg.jos.jossentials.util.MessageDispatcher messageDispatcher;
    private final DeleteConfirmationManager deleteConfirmationManager;
    private final HomesTeleportService teleportService;
    private final HomesSettings settings;

    public HomesSlotItem(Jossentials plugin, HomesService homesService, HomesItemFactory itemFactory,
                         Player player, int slot, String permission, Map<Integer, HomeLocation> homes,
                         gg.jos.jossentials.util.MessageDispatcher messageDispatcher,
                         DeleteConfirmationManager deleteConfirmationManager,
                         HomesTeleportService teleportService,
                         HomesSettings settings) {
        this.plugin = plugin;
        this.homesService = homesService;
        this.itemFactory = itemFactory;
        this.player = player;
        this.slot = slot;
        this.permission = permission;
        this.homes = homes;
        this.messageDispatcher = messageDispatcher;
        this.deleteConfirmationManager = deleteConfirmationManager;
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
        if (settings.deleteConfirmationEnabled) {
            long windowMillis = settings.deleteConfirmationWindowSeconds * 1000L;
            if (deleteConfirmationManager.isPending(player.getUniqueId(), slot, windowMillis)) {
                return itemFactory.create("homes.gui.items.delete-confirm", slot, permission);
            }
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
            HomeLocation location = HomeLocation.fromLocation(player.getLocation());
            homesService.setHome(player.getUniqueId(), slot, location).whenComplete((success, throwable) -> {
                plugin.scheduler().runEntity(player, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (throwable != null) {
                        messageDispatcher.send(player, "messages.home-set-failed", "<red>Failed to set home.");
                        return;
                    }
                    homes.put(slot, location);
                    notifyWindows();
                    String message = plugin.configs().messages().getString("messages.home-set", "<green>Home <gold>%slot%</gold> set.");
                    messageDispatcher.sendWithKey(player, "messages.home-set", message.replace("%slot%", String.valueOf(slot)));
                });
            });
            return;
        }
        if (clickType.isRightClick()) {
            if (settings.deleteConfirmationEnabled) {
                int windowSeconds = settings.deleteConfirmationWindowSeconds;
                if (!deleteConfirmationManager.confirm(player.getUniqueId(), slot, windowSeconds * 1000L)) {
                    String message = plugin.configs().messages().getString("messages.home-delete-confirm", "<yellow>Right-click again to delete.");
                    message = message.replace("%seconds%", String.valueOf(windowSeconds))
                        .replace("%slot%", String.valueOf(slot));
                    messageDispatcher.sendWithKey(player, "messages.home-delete-confirm", message);
                    notifyWindows();
                    plugin.scheduler().runEntityLater(player, this::notifyWindows, windowSeconds * 20L);
                    return;
                }
            }
            homesService.deleteHome(player.getUniqueId(), slot).whenComplete((success, throwable) -> {
                plugin.scheduler().runEntity(player, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (throwable != null) {
                        messageDispatcher.send(player, "messages.home-delete-failed", "<red>Failed to delete home.");
                        return;
                    }
                    homes.remove(slot);
                    deleteConfirmationManager.clear(player.getUniqueId(), slot);
                    notifyWindows();
                    String message = plugin.configs().messages().getString("messages.home-deleted", "<green>Home <gold>%slot%</gold> deleted.");
                    messageDispatcher.sendWithKey(player, "messages.home-deleted", message.replace("%slot%", String.valueOf(slot)));
                });
            });
            return;
        }
        if (clickType.isLeftClick()) {
            var location = existing.toLocation();
            if (location == null) {
                messageDispatcher.send(player, "messages.world-missing", "<red>That world is no longer available.");
                return;
            }
            player.closeInventory();
            teleportService.teleport(player, location, slot);
        }
    }
}
