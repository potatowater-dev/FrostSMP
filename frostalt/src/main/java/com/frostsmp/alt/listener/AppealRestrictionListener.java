package com.frostsmp.alt.listener;

import com.frostsmp.alt.config.PluginConfig;
import com.frostsmp.alt.service.EnforcementService;
import com.frostsmp.alt.util.MessageUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Locale;

public final class AppealRestrictionListener implements Listener {

    private final PluginConfig config;
    private final EnforcementService enforcementService;

    public AppealRestrictionListener(PluginConfig config, EnforcementService enforcementService) {
        this.config = config;
        this.enforcementService = enforcementService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enforcementService.isInAppealMode(event.getPlayer())) {
            return;
        }
        String label = extractCommandLabel(event.getMessage());
        if (config.isAppealCommandAllowed(label)) {
            return;
        }
        event.setCancelled(true);
        MessageUtil.send(event.getPlayer(), config.getCommandBlockedMessage());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!enforcementService.isInAppealMode(event.getPlayer())) {
            return;
        }
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (enforcementService.isInAppealMode(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (enforcementService.isInAppealMode(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (enforcementService.isInAppealMode(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (enforcementService.isInAppealMode(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && enforcementService.isInAppealMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && enforcementService.isInAppealMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameMode(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (enforcementService.isInAppealMode(player) && player.getGameMode() != GameMode.ADVENTURE) {
            player.setGameMode(GameMode.ADVENTURE);
        }
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
