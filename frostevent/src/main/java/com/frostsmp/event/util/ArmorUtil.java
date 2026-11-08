package com.frostsmp.event.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class ArmorUtil {

    private ArmorUtil() {
    }

    public static boolean isNaked(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getArmorContents()) {
            if (isArmorPiece(item)) {
                return false;
            }
        }
        for (ItemStack item : inventory.getStorageContents()) {
            if (isArmorPiece(item)) {
                return false;
            }
        }
        ItemStack offhand = inventory.getItemInOffHand();
        return !isArmorPiece(offhand);
    }

    public static boolean isArmorPiece(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        EquipmentSlot slot = item.getType().getEquipmentSlot();
        return slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET;
    }
}
