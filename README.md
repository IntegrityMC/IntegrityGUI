# IntegrityGUI

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](./LICENSE)
[![Contributions](https://img.shields.io/badge/Contributions-Welcome-orange.svg)]()

IntegrityGUI is a small Spigot GUI API for building per-player inventories with static items, paged content and click callbacks.

## Requirements

- Java 8+
- Spigot API 1.16.5+
- GUI titles, display names and lore helpers use MiniMessage syntax.

## Features

- Static items that stay visible across every page.
- Page-specific items with optional click actions.
- Built-in multi-page navigation with customizable previous/next buttons.
- Back button helper that closes the GUI and runs a callback.
- Border and row fill helpers for simple layouts.
- Single shared listener for click, drag, close and quit events.
- Per-player active GUI tracking.
- No built-in audio feedback.

## Usage

Direct GUI creation is still supported:

```java
IntegrityGUI gui = new IntegrityGUI(plugin, player, "<gold>Shop</gold>", 6);

ItemStack border = IntegrityGUI.withName(
        new ItemStack(Material.BLACK_STAINED_GLASS_PANE),
        "<dark_gray>-"
);

ItemStack item = IntegrityGUI.withLore(
        IntegrityGUI.withName(new ItemStack(Material.DIAMOND), "<aqua>Diamond"),
        Arrays.asList("<gray>Click to buy", "<green>$100")
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
    protected String title() {
        return "<gold>Shop</gold>";
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void body(IntegrityGUI gui) {
        ItemStack diamond = IntegrityGUI.withName(
                new ItemStack(Material.DIAMOND),
                "<aqua>Diamond"
        );

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

- `new IntegrityGUI(plugin, player, title, rows)` creates a GUI. `title` must be MiniMessage.
- `new IntegrityGUI(plugin, player, title, inventoryType)` creates a fixed-type GUI, for example hopper.
- `IntegrityMenu` is the abstract base class for reusable menus with `title()`, `body(gui)`, `inventoryType()` and `rows()`.
- `setItem(slot, item)` sets a static item.
- `setItem(slot, item, action)` sets a static item with an `InventoryClickEvent` action.
- `addPage()` creates a new page.
- `setPageItem(page, slot, item)` sets an item on a page.
- `setPageItem(page, slot, item, action)` sets a page item with an `InventoryClickEvent` action.
- `setNavigation(prevSlot, prevItem, nextSlot, nextItem)` enables page navigation.
- `setBackButton(slot, item, action)` creates a button that closes the menu and runs a callback.
- `open()` opens the GUI for the player.
- `refresh()` redraws the GUI content.
- `close()` closes the GUI and removes it from the active GUI registry.
- `getCurrentPage()` returns the current page index.
- `getTotalPages()` returns the total page count.
- `getStandardSlots()` returns a centered 4-row content layout.
- `parseMiniMessage(text)` converts MiniMessage text to a legacy Bukkit string.
- `withName(item, displayName)` returns a cloned item with a MiniMessage display name.
- `withLore(item, lore)` returns a cloned item with MiniMessage lore.
