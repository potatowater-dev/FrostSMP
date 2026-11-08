package com.frostsmp.speed.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

public final class SpeedPotionItem {

    public static final NamespacedKey KEY = new NamespacedKey("frost", "speed_potion");

    private SpeedPotionItem() {
    }

    public static boolean isSpeedPotion(ItemStack item) {
        if (item == null || item.getType().isAir() || !(item.getItemMeta() instanceof PotionMeta meta)) {
            return false;
        }
        Byte tagged = meta.getPersistentDataContainer().get(KEY, PersistentDataType.BYTE);
        return tagged != null && tagged == (byte) 1;
    }

    public static boolean isSpeedSplash(ItemStack item) {
        if (item == null || item.getType() != Material.SPLASH_POTION || !(item.getItemMeta() instanceof PotionMeta meta)) {
            return false;
        }
        Byte tagged = meta.getPersistentDataContainer().get(KEY, PersistentDataType.BYTE);
        return tagged != null && tagged == (byte) 1;
    }

    public static ItemStack create(int amount) {
        ItemStack item = new ItemStack(Material.SPLASH_POTION, Math.max(1, amount));
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(PotionType.THICK);
        meta.setColor(Color.fromRGB(0x33EBFF));
        meta.displayName(Component.text("Speed Splash Potion")
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
                .decorate(TextDecoration.BOLD));
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }
}
