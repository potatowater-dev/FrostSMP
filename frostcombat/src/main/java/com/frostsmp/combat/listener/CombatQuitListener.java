package com.frostsmp.combat.listener;

import com.frostsmp.combat.config.PluginConfig;
import com.frostsmp.combat.item.KillToken;
import com.frostsmp.combat.service.CombatService;
import com.frostsmp.combat.service.KillStreakService;
import com.frostsmp.combat.storage.CombatLogStorage;
import com.frostsmp.combat.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerQuitEvent.QuitReason;

public final class CombatQuitListener implements Listener {

    private final CombatService combatService;
    private final PluginConfig config;
    private final CombatLogStorage combatLogStorage;
    private final KillStreakService streaks;

    public CombatQuitListener(
            CombatService combatService,
            PluginConfig config,
            CombatLogStorage combatLogStorage,
            KillStreakService streaks
    ) {
        this.combatService = combatService;
        this.config = config;
        this.combatLogStorage = combatLogStorage;
        this.streaks = streaks;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            combatService.clear(player);
            return;
        }
        if (combatService.isCombatLogExempt()) {
            combatService.clear(player);
            return;
        }
        if (!combatService.isInCombat(player)) {
            return;
        }

        // Timed-out / lag disconnects: do not treat as combat logging.
        if (config.isIgnoreTimeoutCombatLog() && isLikelyLagDisconnect(event)) {
            combatService.clear(player);
            return;
        }

        combatLogStorage.mark(player.getUniqueId());
        event.quitMessage(MessageUtil.parse(MessageUtil.format(
                config.getCombatLogBroadcastMessage(), "player", player.getName())));

        Player lastHitter = combatService.lastHitter(player);
        combatService.beginCombatLog(player.getUniqueId());
        streaks.reset(player.getUniqueId(), player.getName());
        if (lastHitter != null && !lastHitter.equals(player)) {
            int tokens = streaks.registerKill(lastHitter);
            KillToken.reward(lastHitter, player.getLocation(), tokens);
        }
        player.setHealth(0.0);
        combatService.endCombatLog(player.getUniqueId());
        combatService.clear(player);
    }

    /**
     * TIMED_OUT = keep-alive failed (wifi drop / severe lag).
     * Intentional leave is DISCONNECTED; staff kicks are KICKED.
     */
    private static boolean isLikelyLagDisconnect(PlayerQuitEvent event) {
        QuitReason reason = event.getReason();
        return reason == QuitReason.TIMED_OUT || reason == QuitReason.ERRONEOUS_STATE;
    }
}
