package com.frostsmp.killeffect.listener;

import com.frostsmp.killeffect.service.KillEffectService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class KillEffectListener implements Listener {

    private final KillEffectService effects;

    public KillEffectListener(KillEffectService effects) {
        this.effects = effects;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) {
            return;
        }
        effects.playForKiller(killer, victim.getLocation());
    }
}
