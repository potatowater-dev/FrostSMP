package com.frostsmp.crates.gui;

import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.model.LootEntry;
import com.frostsmp.crates.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class CratePreviewGui implements FrostGuiHolder {

    public static final int LOOT_SLOTS = 45;

    private final Inventory inventory;

    public CratePreviewGui(CrateDefinition definition) {
        Component title = MessageUtil.parse(definition.getDisplayName() + " <gray>Preview");
        this.inventory = Bukkit.createInventory(this, 54, title);
        populate(definition);
    }

    private void populate(CrateDefinition definition) {
        int total = definition.getTotalWeight();
        int slot = 0;
        for (LootEntry entry : definition.getLoot()) {
            if (slot >= LOOT_SLOTS) {
                break;
            }
            ItemStack display = entry.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                double percent = total > 0 ? (entry.getWeight() * 100.0 / total) : 0;
                lore.add(MessageUtil.parse("<gray>Weight: <white>" + entry.getWeight()));
                lore.add(MessageUtil.parse("<gray>Chance: <white>" + String.format("%.1f", percent) + "%"));
                meta.lore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(slot++, display);
        }
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int index = slot; index < 54; index++) {
            inventory.setItem(index, filler);
        }
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
