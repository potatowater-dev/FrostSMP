package com.frostsmp.killeffect.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class KillEffectItem {

    private static NamespacedKey key;

    private KillEffectItem() {
    }

    public static void init(JavaPlugin plugin) {
        key = new NamespacedKey(plugin, "kill_effect");
    }

    public static NamespacedKey key() {
        return key;
    }

    public static ItemStack stamp(ItemStack item, String effectId) {
        if (item == null || item.getType().isAir() || effectId == null || effectId.isBlank() || key == null) {
            return item;
        }
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) {
            return copy;
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, effectId.toLowerCase(Locale.ROOT));
        copy.setItemMeta(meta);
        return copy;
    }

    public static String effectId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta() || key == null) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public static boolean isClaimItem(ItemStack item) {
        return effectId(item) != null;
    }
}
