package com.frostsmp.combat.listener;

import com.frostsmp.combat.service.PlayerStatsService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerStatsListener implements Listener {

    private final PlayerStatsService playerStats;

    public PlayerStatsListener(PlayerStatsService playerStats) {
        this.playerStats = playerStats;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        playerStats.handleJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        playerStats.handleQuit(event.getPlayer());
    }
}
