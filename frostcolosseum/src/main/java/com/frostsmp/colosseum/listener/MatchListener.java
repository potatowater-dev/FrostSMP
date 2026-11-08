package com.frostsmp.colosseum.listener;

import com.frostsmp.colosseum.config.PluginConfig;
import com.frostsmp.colosseum.model.CuboidRegion;
import com.frostsmp.colosseum.model.MatchState;
import com.frostsmp.colosseum.service.MatchService;
import com.frostsmp.colosseum.service.QueueService;
import com.frostsmp.colosseum.storage.RegionStorage;
import com.frostsmp.colosseum.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class MatchListener implements Listener {

    private final PluginConfig config;
    private final RegionStorage regionStorage;
    private final QueueService queueService;
    private final MatchService matchService;

    public MatchListener(
            PluginConfig config,
            RegionStorage regionStorage,
            QueueService queueService,
            MatchService matchService
    ) {
        this.config = config;
        this.regionStorage = regionStorage;
        this.queueService = queueService;
        this.matchService = matchService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (queueService.handleMove(player, from, to)) {
            event.setTo(from);
            return;
        }

        if (shouldBlockArenaEntry(player, from, to)) {
            event.setTo(from);
            MessageUtil.send(player, config.msg("arena-blocked"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (shouldBlockArenaEntry(player, from, to)) {
            event.setCancelled(true);
            MessageUtil.send(player, config.msg("arena-blocked"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!matchService.isLiving(player.getUniqueId())) {
            return;
        }
        // Keep vanilla drops in arena; do not keep inventory
        event.setKeepInventory(false);
        event.setKeepLevel(false);
        matchService.handleDeath(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!matchService.isSpectator(player.getUniqueId())
                && !matchService.isParticipant(player.getUniqueId())) {
            return;
        }
        if (matchService.getState() == MatchState.IDLE) {
            return;
        }
        CuboidRegion spectate = regionStorage.getRegion("spectate");
        if (spectate != null) {
            event.setRespawnLocation(spectate.center());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        matchService.handleQuit(event.getPlayer());
        queueService.remove(event.getPlayer().getUniqueId());
    }

    private boolean shouldBlockArenaEntry(Player player, Location from, Location to) {
        MatchState state = matchService.getState();
        if (state == MatchState.IDLE) {
            return false;
        }
        CuboidRegion arena = regionStorage.getRegion("arena");
        if (arena == null) {
            return false;
        }
        boolean entering = arena.contains(to) && (from == null || !arena.contains(from));
        if (!entering) {
            return false;
        }
        return !matchService.canEnterArena(player);
    }
}
