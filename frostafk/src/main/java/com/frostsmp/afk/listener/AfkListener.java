package com.frostsmp.afk.listener;

import com.frostsmp.afk.service.AfkService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class AfkListener implements Listener {

    private final AfkService afkService;

    public AfkListener(AfkService afkService) {
        this.afkService = afkService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        afkService.onMove(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        afkService.onQuit(event.getPlayer());
    }
}
