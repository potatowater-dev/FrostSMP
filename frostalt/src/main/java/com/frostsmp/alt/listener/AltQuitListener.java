package com.frostsmp.alt.listener;

import com.frostsmp.alt.service.OnlineIpRegistry;
import com.frostsmp.alt.service.PlaytimeService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class AltQuitListener implements Listener {

    private final OnlineIpRegistry ipRegistry;
    private final PlaytimeService playtimeService;

    public AltQuitListener(OnlineIpRegistry ipRegistry, PlaytimeService playtimeService) {
        this.ipRegistry = ipRegistry;
        this.playtimeService = playtimeService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        playtimeService.onQuit(event.getPlayer());
        ipRegistry.unregister(event.getPlayer());
    }
}
