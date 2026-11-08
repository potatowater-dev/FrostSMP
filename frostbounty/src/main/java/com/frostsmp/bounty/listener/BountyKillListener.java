package com.frostsmp.bounty.listener;

import com.frostsmp.bounty.service.BountyService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class BountyKillListener implements Listener {

    private final BountyService bountyService;

    public BountyKillListener(BountyService bountyService) {
        this.bountyService = bountyService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        bountyService.claimOnKill(victim, killer);
    }
}
