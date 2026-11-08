package com.frostsmp.pearls.listener;

import com.frostsmp.pearls.config.PluginConfig;
import com.frostsmp.pearls.cooldown.PearlCooldownManager;
import com.frostsmp.pearls.util.MessageUtil;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public final class PearlCooldownListener implements Listener {

    private final PluginConfig config;
    private final PearlCooldownManager cooldownManager;

    public PearlCooldownListener(PluginConfig config, PearlCooldownManager cooldownManager) {
        this.config = config;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPearlLaunch(ProjectileLaunchEvent event) {
        if (!config.isEnabled() || !config.isPearlCooldownEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof EnderPearl)) {
            return;
        }
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }
        if (!player.hasPermission("frostsmp.pearls.use")) {
            return;
        }
        if (player.hasPermission("frostsmp.pearls.bypass.cooldown")) {
            return;
        }
        if (!cooldownManager.isOnCooldown(player)) {
            return;
        }
        event.setCancelled(true);
        String message = config.getPearlCooldownMessage()
                .replace("{seconds}", String.valueOf(cooldownManager.getRemainingSeconds(player)));
        MessageUtil.send(player, message);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPearlLaunched(ProjectileLaunchEvent event) {
        if (!config.isEnabled() || !config.isPearlCooldownEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof EnderPearl)) {
            return;
        }
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }
        if (!player.hasPermission("frostsmp.pearls.use")) {
            return;
        }
        if (player.hasPermission("frostsmp.pearls.bypass.cooldown")) {
            return;
        }
        cooldownManager.setCooldown(player, config.getPearlCooldownMs());
    }
}
