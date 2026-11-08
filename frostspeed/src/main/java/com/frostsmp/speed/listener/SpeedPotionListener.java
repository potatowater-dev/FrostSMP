package com.frostsmp.speed.listener;

import com.frostsmp.speed.item.SpeedPotionItem;
import com.frostsmp.speed.service.SpeedPotionService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class SpeedPotionListener implements Listener {

    private final SpeedPotionService speedPotions;

    public SpeedPotionListener(SpeedPotionService speedPotions) {
        this.speedPotions = speedPotions;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!SpeedPotionItem.isSpeedPotion(event.getItem())) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("frostsmp.speed.use")) {
            return;
        }
        speedPotions.apply(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent event) {
        if (!SpeedPotionItem.isSpeedSplash(event.getPotion().getItem())) {
            return;
        }
        // Cancel any real effects the splash would normally give — we handle it ourselves.
        event.setCancelled(true);
        for (LivingEntity affected : event.getAffectedEntities()) {
            if (!(affected instanceof Player player)) {
                continue;
            }
            if (!player.hasPermission("frostsmp.speed.use")) {
                continue;
            }
            speedPotions.apply(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        speedPotions.clear(event.getPlayer());
    }
}
