package com.frostsmp.afk.service;

import com.frostsmp.afk.FrostAfkPlugin;
import com.frostsmp.afk.config.PluginConfig;
import com.frostsmp.afk.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class WandService {

    public static final String WAND_KEY = "afk_wand";

    private final FrostAfkPlugin plugin;
    private final PluginConfig config;
    private final org.bukkit.NamespacedKey wandKey;

    public WandService(FrostAfkPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.wandKey = new org.bukkit.NamespacedKey(plugin, WAND_KEY);
    }

    public void giveWand(Player player) {
        ItemStack item = new ItemStack(config.getWandMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(config.getWandName()));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : config.getWandLore()) {
                lore.add(MessageUtil.parse(line));
            }
            meta.lore(lore);
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
        MessageUtil.send(player, config.getWandGiven());
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    public FrostAfkPlugin getPlugin() {
        return plugin;
    }
}
