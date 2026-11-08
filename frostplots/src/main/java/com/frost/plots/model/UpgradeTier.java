package com.frost.plots.model;

import org.bukkit.inventory.ItemStack;

public record UpgradeTier(int size, int costAmount, String costItem, ItemStack costTemplate) {

    public boolean isFree() {
        return costAmount <= 0;
    }
}
