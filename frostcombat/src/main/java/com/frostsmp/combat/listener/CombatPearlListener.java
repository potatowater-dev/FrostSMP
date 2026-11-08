package com.frostsmp.combat.listener;

import com.frostsmp.combat.config.PluginConfig;
import com.frostsmp.combat.service.CombatService;
import com.frostsmp.combat.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class CombatPearlListener implements Listener {

    private final CombatService combatService;
    private final PluginConfig config;

    public CombatPearlListener(CombatService combatService, PluginConfig config) {
        this.combatService = combatService;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPearlUse(PlayerInteractEvent event) {
        if (!config.isBlockPearlsInCombat()) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ENDER_PEARL) {
            return;
        }
        Player player = event.getPlayer();
        if (!combatService.isInCombat(player)) {
            return;
        }
        event.setCancelled(true);
        MessageUtil.send(player, config.getPearlBlockedMessage());
    }
}
