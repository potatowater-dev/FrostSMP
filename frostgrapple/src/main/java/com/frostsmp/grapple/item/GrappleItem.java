package com.frostsmp.grapple.item;

import com.frostsmp.grapple.config.GrappleConfig;
import com.frostsmp.grapple.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class GrappleItem {

    public static final NamespacedKey KEY = new NamespacedKey("frost", "grapple");

    private GrappleItem() {
    }

    public static boolean isGrapple(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) {
            return false;
        }
        Byte tagged = item.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.BYTE);
        return tagged != null && tagged == (byte) 1;
    }

    public static ItemStack create(GrappleConfig config, int amount) {
        ItemStack item = new ItemStack(Material.FISHING_ROD, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MessageUtil.parse(config.itemName()).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String line : config.itemLore()) {
            lore.add(MessageUtil.parse(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        meta.setUnbreakable(config.unbreakable());
        if (config.unbreakable()) {
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }
}
