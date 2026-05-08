package gg.jos.jossentials.homes.gui;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.homes.HomeLocation;
import gg.jos.jossentials.homes.HomesService;
import gg.jos.jossentials.homes.HomesSettings;
import gg.jos.jossentials.util.ColorUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HomesGui {
    private static final Pattern PAGE_SUFFIX_PATTERN = Pattern.compile("(\\d+)$");
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
        open(player, player.getUniqueId(), player.getName(), false, 0);
    }

    public void open(Player viewer, UUID playerId, String playerName, boolean readOnly) {
        open(viewer, playerId, playerName, readOnly, 0);
    }

    public void open(Player viewer, UUID playerId, String playerName, boolean readOnly, int page) {
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
                List<PageDefinition> pages = resolvePages();
                int currentPage = Math.max(0, Math.min(page, pages.size() - 1));
                Gui gui = buildGui(viewer, playerId, playerName, readOnly, homes, pages, currentPage);
                String titlePath = readOnly ? "homes.gui.other-title" : "homes.gui.title";
                String title = plugin.configs().homes().getString(titlePath, readOnly ? "<gold>%player%'s Homes" : "<gold>Homes");
                Window window = Window.builder()
                    .setViewer(viewer)
                    .setTitle(ColorUtil.mini(title.replace("%player%", playerName).replace("%page%", String.valueOf(currentPage + 1))))
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

    private Gui buildGui(Player viewer, UUID playerId, String playerName, boolean readOnly, Map<Integer, HomeLocation> homes, List<PageDefinition> pages, int pageIndex) {
        PageDefinition page = pages.get(pageIndex);
        List<String> structure = normalizeStructure(page.rawStructure(), page.rows());
        Set<Character> structureChars = collectStructureChars(structure);
        Map<Character, Item> ingredients = buildIngredientMap(page.path());

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

        int remainingSlots = Math.max(0, settings.maxSlots - page.startSlot());
        int visibleHomes = Math.min(remainingSlots, page.homeSlots().size());
        boolean showLocked = plugin.configs().homes().getBoolean("homes.gui.show-locked-slots", true);

        for (int i = 0; i < visibleHomes; i++) {
            int slotNumber = page.startSlot() + i + 1;
            int index = page.homeSlots().get(i);
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
                settings,
                page.itemPath()
            );
            gui.setItem(index, iconItem);

            if (!readOnly && i < page.actionSlots().size()) {
                int actionIndex = page.actionSlots().get(i);
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
                    iconItem,
                    page.itemPath()
                );
                gui.setItem(actionIndex, actionItem);
            }
        }

        for (Integer index : page.previousSlots()) {
            gui.setItem(index, new PageNavigationItem(viewer, playerId, playerName, readOnly, pageIndex - 1, pageIndex > 0, new String[]{page.navigationPath() + ".previous", "homes.gui.navigation.previous"}));
        }
        for (Integer index : page.nextSlots()) {
            gui.setItem(index, new PageNavigationItem(viewer, playerId, playerName, readOnly, pageIndex + 1, pageIndex + 1 < pages.size(), new String[]{page.navigationPath() + ".next", "homes.gui.navigation.next"}));
        }

        return gui;
    }

    private Map<Character, Item> buildIngredientMap(String pagePath) {
        Map<Character, Item> items = new HashMap<>();
        ConfigurationSection pageSection = plugin.configs().homes().getConfigurationSection(pagePath + ".ingredients");
        if (pageSection != null) {
            addIngredients(items, pageSection, pagePath + ".ingredients");
        }
        ConfigurationSection section = plugin.configs().homes().getConfigurationSection("homes.gui.ingredients");
        if (section != null) {
            addIngredients(items, section, "homes.gui.ingredients");
        }
        return items;
    }

    private void addIngredients(Map<Character, Item> items, ConfigurationSection section, String path) {
        for (String key : section.getKeys(false)) {
            if (key.length() != 1) {
                continue;
            }
            char ch = key.charAt(0);
            ItemProvider provider = itemFactory.create(path + "." + key, -1, "");
            items.put(ch, Item.simple(provider));
        }
    }

    private List<PageDefinition> resolvePages() {
        List<PageDefinition> pages = new ArrayList<>();
        ConfigurationSection pagesSection = plugin.configs().homes().getConfigurationSection("homes.gui.pages");
        if (pagesSection == null) {
            pages.add(createLegacyPageDefinition());
            return pages;
        }

        List<String> pageKeys = new ArrayList<>(pagesSection.getKeys(false));
        pageKeys.sort(Comparator.comparingInt(this::pageOrder).thenComparing(String::compareTo));

        int startSlot = 0;
        for (String pageKey : pageKeys) {
            if (startSlot >= settings.maxSlots) {
                break;
            }
            String pagePath = "homes.gui.pages." + pageKey;
            PageDefinition definition = createPageDefinition(pagePath, startSlot);
            if (definition.homeSlots().isEmpty()) {
                continue;
            }
            pages.add(definition);
            startSlot += definition.homeSlots().size();
        }

        if (pages.isEmpty()) {
            pages.add(createLegacyPageDefinition());
        }
        return pages;
    }

    private PageDefinition createLegacyPageDefinition() {
        List<String> rawStructure = plugin.configs().homes().getStringList("homes.gui.structure");
        if (rawStructure.isEmpty()) {
            rawStructure = buildDefaultStructure();
        }
        int rows = rawStructure.size();
        int size = rows * 9;
        List<Integer> homeSlots = resolveHomeSlots("homes.gui", rawStructure, size);
        List<Integer> actionSlots = resolveActionSlots("homes.gui", rawStructure, homeSlots, size);
        return new PageDefinition(
            "homes.gui",
            "homes.gui.items",
            "homes.gui.navigation",
            rawStructure,
            rows,
            homeSlots,
            actionSlots,
            extractSlotsFromStructure(rawStructure, size, 'P'),
            extractSlotsFromStructure(rawStructure, size, 'N'),
            0
        );
    }

    private PageDefinition createPageDefinition(String pagePath, int startSlot) {
        List<String> rawStructure = plugin.configs().homes().getStringList(pagePath + ".structure");
        if (rawStructure.isEmpty()) {
            rawStructure = buildDefaultStructure();
        }
        int rows = rawStructure.size();
        int size = rows * 9;
        List<Integer> homeSlots = resolveHomeSlots(pagePath, rawStructure, size);
        List<Integer> actionSlots = resolveActionSlots(pagePath, rawStructure, homeSlots, size);
        return new PageDefinition(
            pagePath,
            pagePath + ".items",
            pagePath + ".navigation",
            rawStructure,
            rows,
            homeSlots,
            actionSlots,
            extractSlotsFromStructure(rawStructure, size, 'P'),
            extractSlotsFromStructure(rawStructure, size, 'N'),
            startSlot
        );
    }

    private List<Integer> resolveHomeSlots(String basePath, List<String> rawStructure, int size) {
        List<Integer> homeSlots = plugin.configs().homes().getIntegerList(basePath + ".home-slots");
        if (homeSlots.isEmpty()) {
            homeSlots = plugin.configs().homes().getIntegerList("homes.gui.home-slots");
        }
        if (homeSlots.isEmpty()) {
            homeSlots = plugin.configs().homes().getIntegerList("homes.gui.slot-indices");
        }
        if (homeSlots.isEmpty() && rawStructure != null && !rawStructure.isEmpty()) {
            homeSlots = extractSlotsFromStructure(rawStructure, size, 'H');
        }
        if (homeSlots.isEmpty()) {
            int maxSlots = settings.maxSlots;
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

    private List<Integer> resolveActionSlots(String basePath, List<String> rawStructure, List<Integer> homeSlots, int size) {
        List<Integer> actionSlots = plugin.configs().homes().getIntegerList(basePath + ".action-slots");
        if (actionSlots.isEmpty()) {
            actionSlots = plugin.configs().homes().getIntegerList("homes.gui.action-slots");
        }
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

    private List<String> buildDefaultStructure() {
        return List.of(
            "P.HHHHH.N",
            "..AAAAA.."
        );
    }

    private int pageOrder(String pageKey) {
        Matcher matcher = PAGE_SUFFIX_PATTERN.matcher(pageKey);
        if (!matcher.find()) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private final class PageNavigationItem extends AbstractItem {
        private final Player viewer;
        private final UUID playerId;
        private final String playerName;
        private final boolean readOnly;
        private final int targetPage;
        private final boolean enabled;
        private final String[] paths;

        private PageNavigationItem(Player viewer, UUID playerId, String playerName, boolean readOnly, int targetPage, boolean enabled, String[] paths) {
            this.viewer = viewer;
            this.playerId = playerId;
            this.playerName = playerName;
            this.readOnly = readOnly;
            this.targetPage = targetPage;
            this.enabled = enabled;
            this.paths = paths;
        }

        @Override
        public ItemProvider getItemProvider(Player viewer) {
            String[] resolvedPaths = enabled
                ? paths
                : new String[]{paths[0] + "-disabled", paths[1] + "-disabled", paths[0], paths[1]};
            return itemFactory.create(resolvedPaths, -1, "");
        }

        @Override
        public void handleClick(org.bukkit.event.inventory.ClickType clickType, Player clicker, Click click) {
            if (!enabled || !clickType.isLeftClick() || !clicker.getUniqueId().equals(viewer.getUniqueId())) {
                return;
            }
            open(clicker, playerId, playerName, readOnly, targetPage);
        }
    }

    private record PageDefinition(
        String path,
        String itemPath,
        String navigationPath,
        List<String> rawStructure,
        int rows,
        List<Integer> homeSlots,
        List<Integer> actionSlots,
        List<Integer> previousSlots,
        List<Integer> nextSlots,
        int startSlot
    ) {
    }
}
