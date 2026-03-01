package gg.jos.jossentials.homes.gui;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.homes.HomeLocation;
import gg.jos.jossentials.homes.HomesService;
import gg.jos.jossentials.homes.HomesSettings;
import gg.jos.jossentials.homes.util.DeleteConfirmationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;

import java.util.Locale;
import java.util.Map;

public final class HomesActionButtonItem extends AbstractItem {
    private final Jossentials plugin;
    private final HomesService homesService;
    private final HomesItemFactory itemFactory;
    private final Player player;
    private final int slot;
    private final String permission;
    private final Map<Integer, HomeLocation> homes;
    private final gg.jos.jossentials.util.MessageDispatcher messageDispatcher;
    private final DeleteConfirmationManager deleteConfirmationManager;
    private final HomesSettings settings;
    private final HomesSlotItem iconItem;

    public HomesActionButtonItem(Jossentials plugin, HomesService homesService, HomesItemFactory itemFactory,
                                 Player player, int slot, String permission, Map<Integer, HomeLocation> homes,
                                 gg.jos.jossentials.util.MessageDispatcher messageDispatcher,
                                 DeleteConfirmationManager deleteConfirmationManager,
                                 HomesSettings settings,
                                 HomesSlotItem iconItem) {
        this.plugin = plugin;
        this.homesService = homesService;
        this.itemFactory = itemFactory;
        this.player = player;
        this.slot = slot;
        this.permission = permission;
        this.homes = homes;
        this.messageDispatcher = messageDispatcher;
        this.deleteConfirmationManager = deleteConfirmationManager;
        this.settings = settings;
        this.iconItem = iconItem;
    }

    @Override
    public ItemProvider getItemProvider(Player viewer) {
        if (!player.hasPermission(permission)) {
            return itemFactory.create("homes.gui.items.action-locked", slot, permission);
        }

        HomeLocation location = homes.get(slot);
        if (location == null) {
            return itemFactory.create("homes.gui.items.action-set", slot, permission);
        }

        if (settings.deleteConfirmationEnabled) {
            long windowMillis = settings.deleteConfirmationWindowSeconds * 1000L;
            if (deleteConfirmationManager.isPending(player.getUniqueId(), slot, windowMillis)) {
                return itemFactory.create("homes.gui.items.action-delete-confirm", slot, permission);
            }
        }
        return itemFactory.create("homes.gui.items.action-delete", slot, permission);
    }

    @Override
    public void handleClick(ClickType clickType, Player clicker, Click click) {
        if (!player.isOnline() || !clickType.isLeftClick()) {
            return;
        }

        if (!player.hasPermission(permission)) {
            messageDispatcher.send(player, "messages.no-permission", "<red>You do not have permission.");
            return;
        }

        HomeLocation existing = homes.get(slot);
        if (existing == null) {
            long remainingDelay = deleteConfirmationManager.remainingDeleteSetDelayMillis(
                player.getUniqueId(),
                slot,
                settings.deleteToSetDelayMillis
            );
            if (remainingDelay > 0L) {
                String message = plugin.configs().messages().getString("messages.home-set-delay", "<red>Please wait <gold>%seconds%s</gold> before setting this home again.");
                String seconds = String.format(Locale.US, "%.1f", remainingDelay / 1000.0);
                message = message.replace("%seconds%", seconds).replace("%slot%", String.valueOf(slot));
                messageDispatcher.sendWithKey(player, "messages.home-set-delay", message);
                return;
            }
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
                    deleteConfirmationManager.clear(player.getUniqueId(), slot);
                    deleteConfirmationManager.clearDeleteSetDelay(player.getUniqueId(), slot);
                    refreshAll();
                    String message = plugin.configs().messages().getString("messages.home-set", "<green>Home <gold>%slot%</gold> set.");
                    messageDispatcher.sendWithKey(player, "messages.home-set", message.replace("%slot%", String.valueOf(slot)));
                });
            });
            return;
        }

        if (settings.deleteConfirmationEnabled) {
            int windowSeconds = settings.deleteConfirmationWindowSeconds;
            long windowMillis = windowSeconds * 1000L;
            if (!deleteConfirmationManager.confirm(player.getUniqueId(), slot, windowMillis)) {
                String message = plugin.configs().messages().getString("messages.home-delete-confirm", "<yellow>Click again to delete.");
                message = message.replace("%seconds%", String.valueOf(windowSeconds))
                    .replace("%slot%", String.valueOf(slot));
                messageDispatcher.sendWithKey(player, "messages.home-delete-confirm", message);
                refreshAll();
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
                deleteConfirmationManager.markDeleted(player.getUniqueId(), slot);
                refreshAll();
                String message = plugin.configs().messages().getString("messages.home-deleted", "<green>Home <gold>%slot%</gold> deleted.");
                messageDispatcher.sendWithKey(player, "messages.home-deleted", message.replace("%slot%", String.valueOf(slot)));
            });
        });
    }

    private void refreshAll() {
        notifyWindows();
        iconItem.refresh();
    }
}
