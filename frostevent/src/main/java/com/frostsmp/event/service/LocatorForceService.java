package com.frostsmp.event.service;

import com.frostsmp.event.FrostEventPlugin;
import com.frostsmp.event.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/**
 * While the event runs, strip vanilla waypoint hide modifiers so pumpkin / mob heads / sneaking
 * cannot hide players on the locator bar.
 */
public final class LocatorForceService {

    private static final NamespacedKey HIDE = NamespacedKey.minecraft("waypoint_transmit_range_hide");
    private static final NamespacedKey CROUCH = NamespacedKey.minecraft("waypoint_transmit_range_crouch");

    private final FrostEventPlugin plugin;
    private final PluginConfig config;
    private BukkitTask task;

    public LocatorForceService(FrostEventPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        stop();
        int interval = config.getLocatorTickInterval();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            forceVisible(player);
        }
    }

    public void forceVisible(Player player) {
        AttributeInstance transmit = player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
        if (transmit != null) {
            transmit.removeModifier(HIDE);
            transmit.removeModifier(CROUCH);
            if (transmit.getBaseValue() < 1.0) {
                transmit.setBaseValue(60_000_000.0);
            }
        }
        AttributeInstance receive = player.getAttribute(Attribute.WAYPOINT_RECEIVE_RANGE);
        if (receive != null && receive.getBaseValue() < 1.0) {
            receive.setBaseValue(60_000_000.0);
        }

        // Helmets re-apply hide on equip; strip again and clear invisibility hide path
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null) {
            // Modifiers already stripped above; nothing else required for helmet
        }
        if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY)) {
            // Invisibility also hides waypoints in vanilla — clear during event
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        }
    }
}
