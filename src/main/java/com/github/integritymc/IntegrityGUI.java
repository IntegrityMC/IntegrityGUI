package com.github.integritymc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class IntegrityGUI {

    private static final Map<UUID, IntegrityGUI> activeGuis = new ConcurrentHashMap<>();
    private static final Set<Plugin> listenerPlugins = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final PacketGuiListener PACKET_LISTENER = new PacketGuiListener();
    private static volatile boolean packetListenerRegistered;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final Plugin plugin;
    private final Player player;
    private final String title;
    private final InventoryType inventoryType;
    private final int size;
    private final int rows;

    private Inventory inventory;
    private final Map<Integer, ItemStack> permanentItems;
    private final Map<Integer, Consumer<IntegrityClickEvent>> permanentActions;
    private final List<Page> pages;
    private int currentPage;
    private Navigation navigation;
    private BukkitTask closeTask;
    private boolean closed;

    public IntegrityGUI(Plugin plugin, Player player, String title, int rows) {
        this(plugin, player, title, InventoryType.CHEST, validateRows(rows) * 9, rows);
    }

    public IntegrityGUI(Plugin plugin, Player player, String title, InventoryType inventoryType) {
        this(
                plugin,
                player,
                title,
                validateInventoryType(inventoryType),
                getDefaultSize(inventoryType),
                Math.max(1, getDefaultSize(inventoryType) / 9)
        );
    }

    private IntegrityGUI(Plugin plugin, Player player, String title, InventoryType inventoryType, int size, int rows) {
        if (plugin == null) throw new IllegalArgumentException("Plugin cannot be null");
        if (player == null) throw new IllegalArgumentException("Player cannot be null");
        if (title == null) throw new IllegalArgumentException("Title cannot be null");

        this.plugin = plugin;
        this.player = player;
        this.title = parseMiniMessage(title);
        this.inventoryType = inventoryType;
        this.rows = rows;
        this.size = size;
        this.permanentItems = new HashMap<>();
        this.permanentActions = new HashMap<>();
        this.pages = new ArrayList<>();
        this.currentPage = 0;
        this.closed = false;
    }

    private static int validateRows(int rows) {
        if (rows < 1 || rows > 6) throw new IllegalArgumentException("Rows must be between 1 and 6");
        return rows;
    }

    private static InventoryType validateInventoryType(InventoryType inventoryType) {
        if (inventoryType == null) throw new IllegalArgumentException("Inventory type cannot be null");
        return inventoryType;
    }

    private static int getDefaultSize(InventoryType inventoryType) {
        try {
            Object size = InventoryType.class.getMethod("getDefaultSize").invoke(inventoryType);
            if (size instanceof Integer) {
                return (Integer) size;
            }
        } catch (Exception ignored) {}

        String name = inventoryType.name();
        if ("CHEST".equals(name)) return 27;
        if ("DISPENSER".equals(name) || "DROPPER".equals(name)) return 9;
        if ("FURNACE".equals(name) || "ANVIL".equals(name) || "BREWING".equals(name)) return 3;
        if ("WORKBENCH".equals(name) || "CRAFTING".equals(name)) return 10;
        if ("ENCHANTING".equals(name)) return 2;
        if ("PLAYER".equals(name) || "CREATIVE".equals(name)) return 41;
        if ("MERCHANT".equals(name)) return 3;
        if ("ENDER_CHEST".equals(name)) return 27;
        if ("BEACON".equals(name)) return 1;
        if ("HOPPER".equals(name)) return 5;
        if ("SHULKER_BOX".equals(name)) return 27;
        if ("BARREL".equals(name)) return 27;
        if ("BLAST_FURNACE".equals(name) || "SMOKER".equals(name)) return 3;
        if ("CARTOGRAPHY".equals(name) || "GRINDSTONE".equals(name) || "SMITHING".equals(name) || "STONECUTTER".equals(name)) return 3;
        if ("LOOM".equals(name)) return 4;

        return 9;
    }

    public static String parseMiniMessage(String text) {
        if (text == null) throw new IllegalArgumentException("Text cannot be null");
        return LEGACY_SERIALIZER.serialize(MINI_MESSAGE.deserialize(text));
    }

    public static ItemStack withName(ItemStack item, String displayName) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        if (displayName == null) throw new IllegalArgumentException("Display name cannot be null");

        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(parseMiniMessage(displayName));
            copy.setItemMeta(meta);
        }
        return copy;
    }

    public static ItemStack withLore(ItemStack item, List<String> lore) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        if (lore == null) throw new IllegalArgumentException("Lore cannot be null");

        List<String> parsedLore = new ArrayList<>(lore.size());
        for (String line : lore) {
            parsedLore.add(parseMiniMessage(line));
        }

        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            meta.setLore(parsedLore);
            copy.setItemMeta(meta);
        }
        return copy;
    }

    public IntegrityGUI setItem(int slot, ItemStack item) {
        validateSlot(slot);
        if (item != null) {
            permanentItems.put(slot, item.clone());
        } else {
            permanentItems.remove(slot);
        }
        return this;
    }

    public IntegrityGUI setItem(int slot, ItemStack item, Consumer<IntegrityClickEvent> action) {
        validateSlot(slot);
        if (item != null) {
            permanentItems.put(slot, item.clone());
            if (action != null) {
                permanentActions.put(slot, action);
            } else {
                permanentActions.remove(slot);
            }
        } else {
            permanentItems.remove(slot);
            permanentActions.remove(slot);
        }
        return this;
    }

    public IntegrityGUI fillBorder(ItemStack item) {
        if (item == null) return this;
        ItemStack border = item.clone();

        for (int i = 0; i < 9; i++) {
            permanentItems.put(i, border.clone());
            permanentItems.put((rows - 1) * 9 + i, border.clone());
        }

        for (int i = 1; i < rows - 1; i++) {
            permanentItems.put(i * 9, border.clone());
            permanentItems.put(i * 9 + 8, border.clone());
        }
        return this;
    }

    public IntegrityGUI fillRow(int row, ItemStack item) {
        if (row < 0 || row >= rows) throw new IllegalArgumentException("Invalid row: " + row);
        if (item == null) return this;

        ItemStack fill = item.clone();
        for (int i = 0; i < 9; i++) {
            permanentItems.put(row * 9 + i, fill.clone());
        }
        return this;
    }

    public IntegrityGUI addPage() {
        pages.add(new Page());
        return this;
    }

    public IntegrityGUI setPageItem(int page, int slot, ItemStack item) {
        ensurePage(page);
        validateSlot(slot);
        pages.get(page).setItem(slot, item);
        return this;
    }

    public IntegrityGUI setPageItem(int page, int slot, ItemStack item, Consumer<IntegrityClickEvent> action) {
        ensurePage(page);
        validateSlot(slot);
        pages.get(page).setItem(slot, item, action);
        return this;
    }

    public IntegrityGUI setNavigation(int prevSlot, ItemStack prevItem, int nextSlot, ItemStack nextItem) {
        validateSlot(prevSlot);
        validateSlot(nextSlot);
        if (prevSlot == nextSlot) throw new IllegalArgumentException("Navigation slots must be different");

        this.navigation = new Navigation(
                prevSlot,
                prevItem != null ? prevItem.clone() : null,
                nextSlot,
                nextItem != null ? nextItem.clone() : null
        );

        permanentActions.put(prevSlot, event -> {
            if (currentPage > 0) {
                currentPage--;
                render();
            }
        });

        permanentActions.put(nextSlot, event -> {
            if (currentPage < pages.size() - 1) {
                currentPage++;
                render();
            }
        });

        return this;
    }

    public IntegrityGUI setBackButton(int slot, ItemStack item, Runnable action) {
        return setItem(slot, item, event -> {
            close();
            if (action != null) {
                Bukkit.getScheduler().runTask(plugin, action);
            }
        });
    }

    public void open() {
        if (closed) throw new IllegalStateException("Cannot reopen a closed GUI");
        if (!player.isOnline()) return;

        IntegrityGUI existing = activeGuis.get(player.getUniqueId());
        if (existing != null && existing != this) {
            existing.forceClose();
        }

        if (pages.isEmpty()) addPage();

        registerListener(plugin);

        this.inventory = buildInventory();
        activeGuis.put(player.getUniqueId(), this);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && !closed) {
                player.openInventory(inventory);
            }
        });
    }

    public void close() {
        if (closed) return;

        if (player.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (inventory != null) {
                    player.closeInventory();
                }
            });
        }

        cleanup();
    }

    public void refresh() {
        if (closed || inventory == null) return;
        if (!player.isOnline()) {
            cleanup();
            return;
        }

        render();
    }

    private Inventory buildInventory() {
        Inventory inv = inventoryType == InventoryType.CHEST
                ? Bukkit.createInventory(null, size, title)
                : Bukkit.createInventory(null, inventoryType, title);
        populateInventory(inv);
        return inv;
    }

    private void render() {
        if (inventory == null || closed) return;

        inventory.clear();
        populateInventory(inventory);

        try {
            player.updateInventory();
        } catch (Exception ignored) {}
    }

    private void populateInventory(Inventory inv) {
        if (currentPage < pages.size()) {
            pages.get(currentPage).getItems().forEach((slot, item) -> {
                if (slot >= 0 && slot < size && item != null) {
                    inv.setItem(slot, item.clone());
                }
            });
        }

        permanentItems.forEach((slot, item) -> {
            if (navigation == null || (slot != navigation.prevSlot && slot != navigation.nextSlot)) {
                if (slot >= 0 && slot < size && item != null) {
                    inv.setItem(slot, item.clone());
                }
            }
        });

        if (navigation != null) {
            if (currentPage > 0 && navigation.prevItem != null) {
                inv.setItem(navigation.prevSlot, navigation.prevItem.clone());
            } else if (permanentItems.containsKey(navigation.prevSlot)) {
                ItemStack fallback = permanentItems.get(navigation.prevSlot);
                if (fallback != null) {
                    inv.setItem(navigation.prevSlot, fallback.clone());
                }
            }

            if (currentPage < pages.size() - 1 && navigation.nextItem != null) {
                inv.setItem(navigation.nextSlot, navigation.nextItem.clone());
            } else if (permanentItems.containsKey(navigation.nextSlot)) {
                ItemStack fallback = permanentItems.get(navigation.nextSlot);
                if (fallback != null) {
                    inv.setItem(navigation.nextSlot, fallback.clone());
                }
            }
        }
    }

    private void ensurePage(int page) {
        if (page < 0) throw new IllegalArgumentException("Page cannot be negative");
        while (pages.size() <= page) {
            addPage();
        }
    }

    private void validateSlot(int slot) {
        if (slot < 0 || slot >= size) {
            throw new IllegalArgumentException("Slot " + slot + " is out of bounds for inventory size " + size);
        }
    }

    private void cleanup() {
        closed = true;
        activeGuis.remove(player.getUniqueId());

        if (closeTask != null) {
            try {
                closeTask.cancel();
            } catch (Exception ignored) {}
            closeTask = null;
        }

        inventory = null;
    }

    private void forceClose() {
        if (player.isOnline() && inventory != null) {
            player.closeInventory();
        }
        cleanup();
    }

    private static void registerListener(Plugin plugin) {
        if (listenerPlugins.add(plugin)) {
            plugin.getServer().getPluginManager().registerEvents(new BukkitGuiListener(), plugin);
        }

        if (!packetListenerRegistered) {
            synchronized (IntegrityGUI.class) {
                if (!packetListenerRegistered) {
                    PacketEvents.getAPI().getEventManager().registerListener(PACKET_LISTENER, PacketListenerPriority.HIGHEST);
                    packetListenerRegistered = true;
                }
            }
        }
    }

    private void handlePacketClick(int slot, int button, WrapperPlayClientClickWindow.WindowClickType clickType) {
        if (inventory == null || closed) return;

        if (slot < 0 || slot >= size) {
            syncInventory();
            return;
        }

        ItemStack current = inventory.getItem(slot);
        if (current == null || current.getType() == Material.AIR) {
            syncInventory();
            return;
        }

        IntegrityClickEvent event = new IntegrityClickEvent(player, inventory, slot, button, clickType, current.clone());

        if (permanentActions.containsKey(slot)) {
            Consumer<IntegrityClickEvent> action = permanentActions.get(slot);
            if (action != null) {
                try {
                    action.accept(event);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Error executing GUI action: " + ex.getMessage());
                }
            }
            syncInventory();
            return;
        }

        if (currentPage < pages.size()) {
            Consumer<IntegrityClickEvent> action = pages.get(currentPage).getAction(slot);
            if (action != null) {
                try {
                    action.accept(event);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Error executing page action: " + ex.getMessage());
                }
            }
        }

        syncInventory();
    }

    private void handleClose(InventoryCloseEvent e) {
        if (!e.getPlayer().equals(player)) return;
        if (inventory == null) return;
        if (!isViewingInventory(e.getInventory())) return;

        closeTask = Bukkit.getScheduler().runTaskLater(plugin, this::cleanup, 1L);
    }

    private boolean isViewingInventory(Inventory topInventory) {
        return topInventory != null && topInventory.equals(inventory);
    }

    private void syncInventory() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!closed && player.isOnline()) {
                try {
                    player.setItemOnCursor(new ItemStack(Material.AIR));
                    player.updateInventory();
                } catch (Exception ignored) {}
            }
        });
    }

    private void handleQuit(PlayerQuitEvent e) {
        if (!e.getPlayer().equals(player)) return;
        cleanup();
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return Math.max(1, pages.size());
    }

    public InventoryType getInventoryType() {
        return inventoryType;
    }

    public int getSize() {
        return size;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isClosed() {
        return closed;
    }

    public static IntegrityGUI getActiveGui(Player player) {
        if (player == null) return null;
        return activeGuis.get(player.getUniqueId());
    }

    public static void closeAll() {
        new ArrayList<>(activeGuis.values()).forEach(IntegrityGUI::forceClose);
    }

    public static int[] getStandardSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
    }

    private static class Page {
        private final Map<Integer, ItemStack> items = new HashMap<>();
        private final Map<Integer, Consumer<IntegrityClickEvent>> actions = new HashMap<>();

        public void setItem(int slot, ItemStack item) {
            if (item != null) {
                items.put(slot, item.clone());
            } else {
                items.remove(slot);
            }
        }

        public void setItem(int slot, ItemStack item, Consumer<IntegrityClickEvent> action) {
            if (item != null) {
                items.put(slot, item.clone());
                if (action != null) {
                    actions.put(slot, action);
                } else {
                    actions.remove(slot);
                }
            } else {
                items.remove(slot);
                actions.remove(slot);
            }
        }

        public Map<Integer, ItemStack> getItems() {
            return items;
        }

        public Consumer<IntegrityClickEvent> getAction(int slot) {
            return actions.get(slot);
        }
    }

    private static class Navigation {
        final int prevSlot;
        final ItemStack prevItem;
        final int nextSlot;
        final ItemStack nextItem;

        Navigation(int prevSlot, ItemStack prevItem, int nextSlot, ItemStack nextItem) {
            this.prevSlot = prevSlot;
            this.prevItem = prevItem;
            this.nextSlot = nextSlot;
            this.nextItem = nextItem;
        }
    }

    public static class IntegrityClickEvent {
        private final Player player;
        private final Inventory inventory;
        private final int slot;
        private final int button;
        private final WrapperPlayClientClickWindow.WindowClickType click;
        private final ItemStack currentItem;

        private IntegrityClickEvent(
                Player player,
                Inventory inventory,
                int slot,
                int button,
                WrapperPlayClientClickWindow.WindowClickType click,
                ItemStack currentItem
        ) {
            this.player = player;
            this.inventory = inventory;
            this.slot = slot;
            this.button = button;
            this.click = click;
            this.currentItem = currentItem;
        }

        public Player getPlayer() {
            return player;
        }

        public Player getWhoClicked() {
            return player;
        }

        public Inventory getInventory() {
            return inventory;
        }

        public int getSlot() {
            return slot;
        }

        public int getRawSlot() {
            return slot;
        }

        public int getButton() {
            return button;
        }

        public WrapperPlayClientClickWindow.WindowClickType getClick() {
            return click;
        }

        public ItemStack getCurrentItem() {
            return currentItem.clone();
        }
    }

    private static class PacketGuiListener implements PacketListener {

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;
            if (!(event.getPlayer() instanceof Player)) return;

            Player player = (Player) event.getPlayer();
            IntegrityGUI gui = activeGuis.get(player.getUniqueId());
            if (gui == null || gui.closed || gui.inventory == null) return;

            WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
            event.setCancelled(true);

            Bukkit.getScheduler().runTask(gui.plugin, () -> {
                IntegrityGUI current = activeGuis.get(player.getUniqueId());
                if (current == gui) {
                    gui.handlePacketClick(packet.getSlot(), packet.getButton(), packet.getWindowClickType());
                }
            });
        }
    }

    private static class BukkitGuiListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onClose(InventoryCloseEvent e) {
            IntegrityGUI gui = activeGuis.get(e.getPlayer().getUniqueId());
            if (gui != null) {
                gui.handleClose(e);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onQuit(PlayerQuitEvent e) {
            IntegrityGUI gui = activeGuis.get(e.getPlayer().getUniqueId());
            if (gui != null) {
                gui.handleQuit(e);
            }
        }
    }
}
