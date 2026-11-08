package com.frostsmp.combat.listener;

import com.frostsmp.combat.item.KillToken;
import com.frostsmp.combat.service.CombatService;
import com.frostsmp.combat.service.KillStreakService;
import com.frostsmp.combat.service.PlayerStatsService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class KillTokenListener implements Listener {

    private final CombatService combatService;
    private final KillStreakService streaks;
    private final PlayerStatsService playerStats;

    public KillTokenListener(CombatService combatService, KillStreakService streaks, PlayerStatsService playerStats) {
        this.combatService = combatService;
        this.streaks = streaks;
        this.playerStats = playerStats;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        streaks.reset(victim.getUniqueId(), victim.getName());
        playerStats.recordDeath(victim);

        if (combatService.isCombatLogging(victim.getUniqueId())) {
            return;
        }
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) {
            return;
        }
        int tokens = streaks.registerKill(killer);
        playerStats.recordKill(killer);
        KillToken.reward(killer, killer.getLocation(), tokens);
    }
}
