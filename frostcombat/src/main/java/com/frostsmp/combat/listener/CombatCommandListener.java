package com.frostsmp.combat.listener;

import com.frostsmp.combat.config.PluginConfig;
import com.frostsmp.combat.service.CombatService;
import com.frostsmp.combat.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

public final class CombatCommandListener implements Listener {

    private final CombatService combatService;
    private final PluginConfig config;

    public CombatCommandListener(CombatService combatService, PluginConfig config) {
        this.combatService = combatService;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            return;
        }
        if (!combatService.isInCombat(player)) {
            return;
        }
        String label = extractCommandLabel(event.getMessage());
        if (config.isCommandAllowed(label)) {
            return;
        }
        event.setCancelled(true);
        MessageUtil.send(player, config.getCommandBlockedMessage());
    }

    private static String extractCommandLabel(String message) {
        String trimmed = message.startsWith("/") ? message.substring(1) : message;
        int space = trimmed.indexOf(' ');
        String label = space >= 0 ? trimmed.substring(0, space) : trimmed;
        int namespace = label.indexOf(':');
        if (namespace >= 0) {
            label = label.substring(namespace + 1);
        }
        return label.toLowerCase(Locale.ROOT);
    }
}
