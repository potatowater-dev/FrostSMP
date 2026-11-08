package com.frostsmp.beacons.service;

import com.frostsmp.beacons.config.PluginConfig;
import com.frostsmp.beacons.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class WandService {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final NamespacedKey wandKey;

    public WandService(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.wandKey = new NamespacedKey(plugin, "beacon_wand");
    }

    public NamespacedKey getWandKey() {
        return wandKey;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    public void giveWand(Player player) {
        player.getInventory().addItem(createWand());
        MessageUtil.send(player, config.getWandGiven());
    }

    public ItemStack createWand() {
        ItemStack item = new ItemStack(config.getWandMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(config.getWandName()));
            List<Component> lore = new ArrayList<>();
            for (String line : config.getWandLore()) {
                lore.add(MessageUtil.parse(line));
            }
            meta.lore(lore);
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }
}
