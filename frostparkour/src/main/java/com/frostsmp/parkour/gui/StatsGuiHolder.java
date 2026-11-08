package com.frostsmp.parkour.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class StatsGuiHolder implements InventoryHolder {

    private final UUID targetPlayerId;
    private Inventory inventory;

    public StatsGuiHolder(UUID targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public UUID getTargetPlayerId() {
        return targetPlayerId;
    }

    public void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
