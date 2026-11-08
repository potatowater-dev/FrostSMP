package com.frostsmp.crates.model;

import org.bukkit.inventory.ItemStack;

public final class LootEntry {

    private final ItemStack item;
    private final int weight;

    public LootEntry(ItemStack item, int weight) {
        this.item = item.clone();
        this.weight = Math.max(1, weight);
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public int getWeight() {
        return weight;
    }
}
