package com.github.integritymc;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.Plugin;

public abstract class IntegrityMenu {

    protected final Plugin plugin;
    protected final Player player;

    public IntegrityMenu(Plugin plugin, Player player) {
        if (plugin == null) throw new IllegalArgumentException("Plugin cannot be null");
        if (player == null) throw new IllegalArgumentException("Player cannot be null");

        this.plugin = plugin;
        this.player = player;
    }

    protected String title() {
        return null;
    }

    protected Component titleComponent() {
        String title = title();
        if (title == null) {
            throw new IllegalStateException("Menus must override title() or titleComponent()");
        }
        return IntegrityGUI.miniMessage(title);
    }

    protected abstract void body(IntegrityGUI gui);

    protected InventoryType inventoryType() {
        return InventoryType.CHEST;
    }

    protected int rows() {
        return 6;
    }

    public IntegrityGUI create() {
        IntegrityGUI gui = inventoryType() == InventoryType.CHEST
                ? new IntegrityGUI(plugin, player, titleComponent(), rows())
                : new IntegrityGUI(plugin, player, titleComponent(), inventoryType());

        body(gui);
        return gui;
    }

    public void open() {
        create().open();
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Player getPlayer() {
        return player;
    }
}
