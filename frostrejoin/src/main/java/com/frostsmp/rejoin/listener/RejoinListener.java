package com.frostsmp.rejoin.listener;

import com.frostsmp.rejoin.service.RejoinService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class RejoinListener implements Listener {

    private final RejoinService rejoinService;

    public RejoinListener(RejoinService rejoinService) {
        this.rejoinService = rejoinService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        rejoinService.onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        rejoinService.onPlayerQuit(event.getPlayer());
    }
}
