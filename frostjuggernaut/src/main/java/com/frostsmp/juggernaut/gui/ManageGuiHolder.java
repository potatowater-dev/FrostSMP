package com.frostsmp.juggernaut.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface ManageGuiHolder extends InventoryHolder {
    Inventory getInventory();
}
