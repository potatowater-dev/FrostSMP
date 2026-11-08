package com.frostsmp.combat.service;

import com.frostsmp.combat.config.PluginConfig;
import com.frostsmp.combat.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpawnService {

    private static final class PendingSpawn {
        private final Location anchor;
        private final Location destination;
        private int ticksRemaining;

        private PendingSpawn(Location anchor, Location destination, int ticksRemaining) {
            this.anchor = anchor;
            this.destination = destination;
            this.ticksRemaining = ticksRemaining;
        }
    }

    private final PluginConfig config;
    private final CombatService combatService;
    private final Map<UUID, PendingSpawn> pending = new ConcurrentHashMap<>();

    public SpawnService(PluginConfig config, CombatService combatService) {
        this.config = config;
        this.combatService = combatService;
    }

    public boolean isPending(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    public void startTeleport(Player player) {
        if (combatService.isInCombat(player)) {
            MessageUtil.send(player, config.getSpawnCombatBlockedMessage());
            return;
        }
        if (isPending(player)) {
            MessageUtil.send(player, config.getSpawnAlreadyPendingMessage());
            return;
        }
        Location destination = config.resolveSpawnLocation();
        int delayTicks = config.getSpawnDelaySeconds() * 20;
        pending.put(player.getUniqueId(), new PendingSpawn(player.getLocation().clone(), destination, delayTicks));
        MessageUtil.send(player, config.getSpawnStartedMessage());
    }

    public void cancel(Player player, String message) {
        if (pending.remove(player.getUniqueId()) != null && message != null) {
            MessageUtil.send(player, message);
        }
    }

    public void tick() {
        Iterator<Map.Entry<UUID, PendingSpawn>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingSpawn> entry = iterator.next();
            Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
            PendingSpawn spawn = entry.getValue();

            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }
            if (combatService.isInCombat(player)) {
                iterator.remove();
                MessageUtil.send(player, config.getSpawnCancelledCombatMessage());
                continue;
            }
            if (hasMoved(player, spawn.anchor)) {
                iterator.remove();
                MessageUtil.send(player, config.getSpawnCancelledMoveMessage());
                continue;
            }

            spawn.ticksRemaining--;
            int secondsLeft = (int) Math.ceil(spawn.ticksRemaining / 20.0);
            player.sendActionBar(MessageUtil.parse(
                    config.getSpawnCountdownActionBar().replace("{seconds}", String.valueOf(Math.max(0, secondsLeft)))
            ));

            if (spawn.ticksRemaining <= 0) {
                iterator.remove();
                player.teleport(spawn.destination);
                MessageUtil.send(player, config.getSpawnTeleportedMessage());
            }
        }
    }

    public void clear(Player player) {
        pending.remove(player.getUniqueId());
        player.sendActionBar(Component.empty());
    }

    private static boolean hasMoved(Player player, Location anchor) {
        Location current = player.getLocation();
        return current.getBlockX() != anchor.getBlockX()
                || current.getBlockY() != anchor.getBlockY()
                || current.getBlockZ() != anchor.getBlockZ();
    }
}
