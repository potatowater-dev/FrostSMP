package com.frostsmp.bounty.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface BountyGuiHolder extends InventoryHolder {
    Inventory getInventory();
}
