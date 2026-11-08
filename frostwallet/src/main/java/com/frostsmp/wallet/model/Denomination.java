package com.frostsmp.wallet.model;

import org.bukkit.inventory.ItemStack;

public record Denomination(String id, long worth, ItemStack template) {
}
