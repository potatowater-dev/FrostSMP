package com.frostsmp.crates.gui;

import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class CrateClaimGui implements FrostGuiHolder {

    public static final int SPIN_START = 10;
    public static final int SPIN_END = 16;
    public static final int POINTER_SLOT = 13;

    private final CrateDefinition definition;
    private final Inventory inventory;

    public CrateClaimGui(CrateDefinition definition) {
        this.definition = definition;
        this.inventory = Bukkit.createInventory(this, 27, MessageUtil.parse(definition.getDisplayName() + " <gold>Opening..."));
        fillBorder();
    }

    public CrateDefinition getDefinition() {
        return definition;
    }

    private void fillBorder() {
        ItemStack glass = pane(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack pointer = pane(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta pointerMeta = pointer.getItemMeta();
        if (pointerMeta != null) {
            pointerMeta.displayName(MessageUtil.parse("<gold>▼ Winner ▼"));
            pointer.setItemMeta(pointerMeta);
        }
        for (int slot = 0; slot < 27; slot++) {
            if (slot >= SPIN_START && slot <= SPIN_END) {
                continue;
            }
            inventory.setItem(slot, slot == 22 ? pointer : glass);
        }
    }

    public boolean isOpenFor(Player player) {
        return player.getOpenInventory().getTopInventory().getHolder() == this;
    }

    public void syncSpinRow(Player player, ItemStack[] items) {
        for (int slot = SPIN_START; slot <= SPIN_END; slot++) {
            int index = slot - SPIN_START;
            ItemStack item = index < items.length ? items[index] : null;
            inventory.setItem(slot, item);
        }
    }

    public void syncHighlightWinner(Player player) {
        ItemStack highlight = pane(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = highlight.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<green>Winner!"));
            highlight.setItemMeta(meta);
        }
        inventory.setItem(4, highlight);
        inventory.setItem(22, highlight);
    }

    private ItemStack pane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
