package com.frostsmp.parkour.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class LeaderboardGuiHolder implements InventoryHolder {

    private final String courseId;
    private Inventory inventory;

    public LeaderboardGuiHolder(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
