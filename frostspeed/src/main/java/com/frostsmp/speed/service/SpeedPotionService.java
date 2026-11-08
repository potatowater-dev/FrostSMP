package com.frostsmp.speed.service;

import com.frostsmp.speed.config.SpeedPotionConfig;
import com.frostsmp.speed.item.SpeedPotionItem;
import com.frostsmp.speed.util.MessageUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpeedPotionService {

    private final JavaPlugin plugin;
    private final SpeedPotionConfig config;
    private final Map<UUID, BukkitTask> expiry = new ConcurrentHashMap<>();

    public SpeedPotionService(JavaPlugin plugin, SpeedPotionConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void apply(Player player) {
        cancelExpiry(player.getUniqueId());
        // Remove any existing real Speed effect so we control the icon.
        player.removePotionEffect(PotionEffectType.SPEED);
        applyModifier(player);
        applyIconEffect(player);
        int ticks = config.durationTicks();
        MessageUtil.send(player, config.prefixed("drunk", "seconds", String.valueOf(Math.max(1, ticks / 20))));
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> expire(player.getUniqueId()), ticks);
        expiry.put(player.getUniqueId(), task);
    }

    public void clear(Player player) {
        cancelExpiry(player.getUniqueId());
        removeModifier(player);
        player.removePotionEffect(PotionEffectType.SPEED);
    }

    public void clearAll() {
        for (UUID id : expiry.keySet()) {
            cancelExpiry(id);
            Player player = plugin.getServer().getPlayer(id);
            if (player != null && player.isOnline()) {
                removeModifier(player);
                player.removePotionEffect(PotionEffectType.SPEED);
            }
        }
        expiry.clear();
    }

    private void expire(UUID playerId) {
        expiry.remove(playerId);
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        removeModifier(player);
        player.removePotionEffect(PotionEffectType.SPEED);
    }

    private void applyModifier(Player player) {
        AttributeInstance instance = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (instance == null) {
            return;
        }
        instance.removeModifier(SpeedPotionItem.KEY);
        // Transient: never written to player data if they disconnect mid-boost.
        instance.addTransientModifier(new AttributeModifier(
                SpeedPotionItem.KEY,
                config.magnitude(),
                config.operation()
        ));
    }

    private void applyIconEffect(Player player) {
        int ticks = config.durationTicks();
        int amplifier = config.amplifier();
        // ambient=false, particles=true, icon=true — shows on the HUD and gives bubbles.
        // The real movement speed comes from the AttributeModifier above, not this effect.
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                ticks,
                amplifier,
                false,  // not ambient (beacon-style dimmed particles)
                true,   // show particles
                true    // show HUD icon
        ));
    }

    private void removeModifier(Player player) {
        AttributeInstance instance = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (instance != null) {
            instance.removeModifier(SpeedPotionItem.KEY);
        }
    }

    private void cancelExpiry(UUID playerId) {
        BukkitTask task = expiry.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
}
