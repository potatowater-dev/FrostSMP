package com.frostsmp.crates.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface FrostGuiHolder extends InventoryHolder {
    Inventory getInventory();
}
