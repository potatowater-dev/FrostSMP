package com.frostsmp.combat.listener;

import com.frostsmp.combat.config.PluginConfig;
import com.frostsmp.combat.storage.CombatLogStorage;
import com.frostsmp.combat.util.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class CombatJoinListener implements Listener {

    private final CombatLogStorage combatLogStorage;
    private final PluginConfig config;

    public CombatJoinListener(CombatLogStorage combatLogStorage, PluginConfig config) {
        this.combatLogStorage = combatLogStorage;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (combatLogStorage.consume(event.getPlayer().getUniqueId())) {
            MessageUtil.send(event.getPlayer(), config.getCombatLogPenaltyMessage());
        }
    }
}
