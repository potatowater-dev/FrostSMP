package com.frostsmp.crates.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class PluginKeys {

    public final NamespacedKey crateWand;
    public final NamespacedKey crateId;
    public final NamespacedKey crateBlock;
    public final NamespacedKey hologramPlacedId;
    public final NamespacedKey hologramLine;

    public PluginKeys(Plugin plugin) {
        this.crateWand = new NamespacedKey(plugin, "crate_wand");
        this.crateId = new NamespacedKey(plugin, "crate_id");
        this.crateBlock = new NamespacedKey(plugin, "crate_block");
        this.hologramPlacedId = new NamespacedKey(plugin, "hologram_placed_id");
        this.hologramLine = new NamespacedKey(plugin, "hologram_line");
    }
}
