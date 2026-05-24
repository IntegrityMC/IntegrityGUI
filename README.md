# IntegrityGUI

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](./LICENSE)
[![Contributions](https://img.shields.io/badge/Contributions-Welcome-orange.svg)]()

IntegrityGUI is a small Spigot GUI API for building per-player inventories with static items, paged content and click callbacks.

## Requirements

- Java 8+
- Spigot/Paper 1.8+
- PacketEvents 2.x installed on the server or provided by the plugin using this library.
- GUI titles, display names and lore helpers accept Adventure `Component`.
- String overloads are still available and are parsed as MiniMessage.

## Features

- Static items that stay visible across every page.
- Page-specific items with optional click actions.
- Built-in multi-page navigation with customizable previous/next buttons.
- Back button helper that closes the GUI and runs a callback.
- Border and row fill helpers for simple layouts.
- PacketEvents-based click handling to cancel inventory movement before Bukkit processes it.
- Single shared listener for close and quit cleanup.
- Per-player active GUI tracking.
- No built-in audio feedback.
- Compiled against Spigot API 1.8.8 for broad runtime compatibility.

## Usage

Direct GUI creation is still supported:

```java
IntegrityGUI gui = new IntegrityGUI(plugin, player, "<gold>Shop</gold>", 6);

ItemStack border = IntegrityGUI.withName(new ItemStack(Material.STONE), Component.text("-").color(NamedTextColor.DARK_GRAY));

ItemStack item = IntegrityGUI.withLore(
        IntegrityGUI.withName(new ItemStack(Material.DIAMOND), Component.text("Diamond").color(NamedTextColor.AQUA)),
        Component.text("Click to buy").color(NamedTextColor.GRAY),
        Component.text("$100").color(NamedTextColor.GREEN)
);

gui.fillBorder(border)
        .setItem(22, item, event -> player.sendMessage("Bought with " + event.getClick()))
        .open();
```

For reusable menus, extend `IntegrityMenu`:

```java
public class ShopMenu extends IntegrityMenu {

    public ShopMenu(Plugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component titleComponent() {
        return Component.text("Shop").color(NamedTextColor.GOLD);
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void body(IntegrityGUI gui) {
        ItemStack diamond = IntegrityGUI.withName(new ItemStack(Material.DIAMOND), Component.text("Diamond").color(NamedTextColor.AQUA));

        gui.setItem(22, diamond, event -> {
            Player clicker = (Player) event.getWhoClicked();
            clicker.sendMessage("Bought with " + event.getClick());
        });
    }
}
```

For fixed inventory types like hopper:

```java
public class HopperMenu extends IntegrityMenu {

    public HopperMenu(Plugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "<green>Confirm</green>";
    }

    @Override
    protected InventoryType inventoryType() {
        return InventoryType.HOPPER;
    }

    @Override
    protected void body(IntegrityGUI gui) {
        gui.setItem(2, new ItemStack(Material.EMERALD), event -> event.getWhoClicked().closeInventory());
    }
}
```

Open a reusable menu with:

```java
new ShopMenu(plugin, player).open();
```

## API

- `new IntegrityGUI(plugin, player, title, rows)` creates a GUI. `title` can be an Adventure `Component` or a MiniMessage `String`.
- `new IntegrityGUI(plugin, player, title, inventoryType)` creates a fixed-type GUI, for example hopper.
- `IntegrityMenu` is the base class for reusable menus with `titleComponent()` or `title()`, `body(gui)`, `inventoryType()` and `rows()`.
- `setItem(slot, item)` sets a static item.
- `setItem(slot, item, action)` sets a static item with an `IntegrityClickEvent` action.
- `addPage()` creates a new page.
- `setPageItem(page, slot, item)` sets an item on a page.
- `setPageItem(page, slot, item, action)` sets a page item with an `IntegrityClickEvent` action.
- `setNavigation(prevSlot, prevItem, nextSlot, nextItem)` enables page navigation.
- `setBackButton(slot, item, action)` creates a button that closes the menu and runs a callback.
- `open()` opens the GUI for the player.
- `refresh()` redraws the GUI content.
- `close()` closes the GUI and removes it from the active GUI registry.
- `getCurrentPage()` returns the current page index.
- `getTotalPages()` returns the total page count.
- `getStandardSlots()` returns a centered 4-row content layout.
- `miniMessage(text)` converts MiniMessage text to an Adventure `Component`.
- `parseMiniMessage(text)` converts MiniMessage text to a legacy Bukkit string.
- `serializeComponent(component)` converts an Adventure `Component` to a legacy Bukkit string.
- `withName(item, displayName)` returns a cloned item with a `Component` or MiniMessage display name.
- `withLore(item, lore)` returns a cloned item with `Component` or MiniMessage lore.
