package com.frostsmp.combat.listener;

import com.frostsmp.combat.config.PluginConfig;
import com.frostsmp.combat.service.SpawnService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class SpawnListener implements Listener {

    private final SpawnService spawnService;
    private final PluginConfig config;

    public SpawnListener(SpawnService spawnService, PluginConfig config) {
        this.spawnService = spawnService;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && spawnService.isPending(player)) {
            spawnService.cancel(player, config.getSpawnCancelledDamageMessage());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        spawnService.clear(event.getPlayer());
    }
}
