package gg.jos.jossentials.homes.gui;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.homes.HomeLocation;
import gg.jos.jossentials.homes.HomesService;
import gg.jos.jossentials.homes.HomesSettings;
import gg.jos.jossentials.util.ColorUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HomesGui {
    private final Jossentials plugin;
    private final HomesService homesService;
    private final HomesItemFactory itemFactory;
    private final gg.jos.jossentials.util.MessageDispatcher messageDispatcher;
    private final gg.jos.jossentials.homes.util.DeleteConfirmationManager deleteConfirmationManager;
    private final gg.jos.jossentials.homes.teleport.HomesTeleportService teleportService;
    private volatile HomesSettings settings;

    public HomesGui(Jossentials plugin, HomesService homesService, gg.jos.jossentials.util.MessageDispatcher messageDispatcher,
                    gg.jos.jossentials.homes.util.DeleteConfirmationManager deleteConfirmationManager,
                    gg.jos.jossentials.homes.teleport.HomesTeleportService teleportService,
                    HomesSettings settings) {
        this.plugin = plugin;
        this.homesService = homesService;
        this.itemFactory = new HomesItemFactory(plugin);
        this.messageDispatcher = messageDispatcher;
        this.deleteConfirmationManager = deleteConfirmationManager;
        this.teleportService = teleportService;
        this.settings = settings;
    }

    public void open(Player player) {
        open(player, player.getUniqueId(), player.getName(), false);
    }

    public void open(Player viewer, UUID playerId, String playerName, boolean readOnly) {
        messageDispatcher.send(viewer, "messages.loading", "");
        homesService.loadHomes(playerId).whenComplete((homes, throwable) -> {
            plugin.scheduler().runEntity(viewer, () -> {
                if (!viewer.isOnline()) {
                    return;
                }
                if (throwable != null) {
                    messageDispatcher.send(viewer, "messages.database-error", "<red>Could not load homes.");
                    return;
                }
                Gui gui = buildGui(viewer, playerId, playerName, readOnly, homes);
                String titlePath = readOnly ? "homes.gui.other-title" : "homes.gui.title";
                String title = plugin.configs().homes().getString(titlePath, readOnly ? "<gold>%player%'s Homes" : "<gold>Homes");
                Window window = Window.builder()
                    .setViewer(viewer)
                    .setTitle(ColorUtil.mini(title.replace("%player%", playerName)))
                    .setUpperGui(gui)
                    .build();
                window.open();
            });
        });
    }

    public void reload(HomesSettings settings) {
        itemFactory.clearCache();
        this.settings = settings;
    }

    private Gui buildGui(Player viewer, UUID playerId, String playerName, boolean readOnly, Map<Integer, HomeLocation> homes) {
        List<String> rawStructure = plugin.configs().homes().getStringList("homes.gui.structure");
        boolean hasStructure = rawStructure != null && !rawStructure.isEmpty();
        int rows = hasStructure ? rawStructure.size() : normalizedSize(plugin.configs().homes().getInt("homes.gui.size", 27)) / 9;
        int size = rows * 9;
        List<String> structure = normalizeStructure(rawStructure, rows);
        Set<Character> structureChars = collectStructureChars(structure);
        Map<Character, Item> ingredients = buildIngredientMap();

        Gui.Builder builder = Gui.builder().setStructure(structure.toArray(new String[0]));
        for (Map.Entry<Character, Item> entry : ingredients.entrySet()) {
            builder.addIngredient(entry.getKey(), entry.getValue());
        }
        for (char ch : structureChars) {
            if (!ingredients.containsKey(ch)) {
                builder.addIngredient(ch, Item.simple(ItemProvider.EMPTY));
            }
        }

        Gui gui = builder.build();

        List<Integer> homeSlots = resolveHomeSlots(rawStructure, size);
        List<Integer> actionSlots = resolveActionSlots(homeSlots, size);
        int maxSlots = plugin.configs().homes().getInt("homes.max-slots", 5);
        maxSlots = Math.min(maxSlots, homeSlots.size());
        boolean showLocked = plugin.configs().homes().getBoolean("homes.gui.show-locked-slots", true);

        for (int i = 0; i < maxSlots; i++) {
            int slotNumber = i + 1;
            int index = homeSlots.get(i);
            String permission = "jossentials.homes." + slotNumber;
            boolean hasHome = homes.containsKey(slotNumber);
            boolean hasPermission = readOnly || viewer.hasPermission(permission);
            if (!hasPermission && !hasHome && !showLocked) {
                continue;
            }
            HomesSlotItem iconItem = new HomesSlotItem(
                itemFactory,
                viewer,
                readOnly,
                slotNumber,
                permission,
                homes,
                messageDispatcher,
                teleportService,
                settings
            );
            gui.setItem(index, iconItem);

            if (!readOnly && i < actionSlots.size()) {
                int actionIndex = actionSlots.get(i);
                Item actionItem = new HomesActionButtonItem(
                    plugin,
                    homesService,
                    itemFactory,
                    viewer,
                    playerId,
                    slotNumber,
                    permission,
                    homes,
                    messageDispatcher,
                    deleteConfirmationManager,
                    settings,
                    iconItem
                );
                gui.setItem(actionIndex, actionItem);
            }
        }

        return gui;
    }

    private Map<Character, Item> buildIngredientMap() {
        Map<Character, Item> items = new HashMap<>();
        ConfigurationSection section = plugin.configs().homes().getConfigurationSection("homes.gui.ingredients");
        if (section == null) {
            return items;
        }
        for (String key : section.getKeys(false)) {
            if (key.length() != 1) {
                continue;
            }
            char ch = key.charAt(0);
            ItemProvider provider = itemFactory.create("homes.gui.ingredients." + key, -1, "");
            items.put(ch, Item.simple(provider));
        }
        return items;
    }

    private List<Integer> resolveHomeSlots(List<String> rawStructure, int size) {
        List<Integer> homeSlots = plugin.configs().homes().getIntegerList("homes.gui.home-slots");
        if (homeSlots.isEmpty()) {
            homeSlots = plugin.configs().homes().getIntegerList("homes.gui.slot-indices");
        }
        if (homeSlots.isEmpty() && rawStructure != null && !rawStructure.isEmpty()) {
            homeSlots = extractSlotsFromStructure(rawStructure, size, 'H');
        }
        if (homeSlots.isEmpty()) {
            int maxSlots = plugin.configs().homes().getInt("homes.max-slots", 5);
            maxSlots = Math.min(maxSlots, size);
            for (int i = 0; i < maxSlots; i++) {
                homeSlots.add(i);
            }
        }
        List<Integer> validated = new ArrayList<>();
        for (Integer index : homeSlots) {
            if (index != null && index >= 0 && index < size) {
                validated.add(index);
            }
        }
        return validated;
    }

    private List<Integer> resolveActionSlots(List<Integer> homeSlots, int size) {
        List<Integer> actionSlots = plugin.configs().homes().getIntegerList("homes.gui.action-slots");
        List<String> rawStructure = plugin.configs().homes().getStringList("homes.gui.structure");
        if (actionSlots.isEmpty() && rawStructure != null && !rawStructure.isEmpty()) {
            actionSlots = extractSlotsFromStructure(rawStructure, size, 'A');
        }
        if (actionSlots.isEmpty() && (rawStructure == null || rawStructure.isEmpty())) {
            for (Integer homeSlot : homeSlots) {
                if (homeSlot == null) {
                    continue;
                }
                int below = homeSlot + 9;
                if (below >= 0 && below < size) {
                    actionSlots.add(below);
                }
            }
        }
        List<Integer> validated = new ArrayList<>();
        for (Integer index : actionSlots) {
            if (index != null && index >= 0 && index < size) {
                validated.add(index);
            }
        }
        return validated;
    }

    private List<Integer> extractSlotsFromStructure(List<String> rawStructure, int size, char target) {
        List<Integer> indices = new ArrayList<>();
        int rows = rawStructure.size();
        for (int row = 0; row < rows; row++) {
            String line = rawStructure.get(row).replace(" ", "");
            for (int col = 0; col < line.length() && col < 9; col++) {
                if (line.charAt(col) == target) {
                    int index = row * 9 + col;
                    if (index >= 0 && index < size) {
                        indices.add(index);
                    }
                }
            }
        }
        return indices;
    }

    private List<String> normalizeStructure(List<String> rawStructure, int rows) {
        List<String> normalized = new ArrayList<>();
        if (rawStructure == null || rawStructure.isEmpty()) {
            for (int row = 0; row < rows; row++) {
                normalized.add(spacedRow("........."));
            }
            return normalized;
        }
        for (int row = 0; row < rows; row++) {
            String line = row < rawStructure.size() ? rawStructure.get(row) : "";
            line = line.replace(" ", "");
            if (line.length() < 9) {
                line = line + ".".repeat(9 - line.length());
            } else if (line.length() > 9) {
                line = line.substring(0, 9);
            }
            normalized.add(spacedRow(line));
        }
        return normalized;
    }

    private Set<Character> collectStructureChars(List<String> structure) {
        Set<Character> chars = new HashSet<>();
        for (String row : structure) {
            String compact = row.replace(" ", "");
            for (int i = 0; i < compact.length(); i++) {
                chars.add(compact.charAt(i));
            }
        }
        return chars;
    }

    private String spacedRow(String row) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < row.length(); i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(row.charAt(i));
        }
        return builder.toString();
    }

    private int normalizedSize(int size) {
        int normalized = Math.max(9, Math.min(54, size));
        if (normalized % 9 != 0) {
            normalized = ((normalized / 9) + 1) * 9;
        }
        return normalized;
    }
}
