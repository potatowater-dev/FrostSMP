package com.frostsmp.event.service;

import com.frostsmp.event.config.PluginConfig;
import com.frostsmp.event.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * During the event: players cannot take PvP damage for a short period after respawn/event start,
 * until they hit another player (or the timer expires). Disconnect clears protection.
 */
public final class SpawnProtectionService {

    private final PluginConfig config;
    private final Map<UUID, Long> protectedUntilMs = new ConcurrentHashMap<>();

    public SpawnProtectionService(PluginConfig config) {
        this.config = config;
    }

    public void clear() {
        protectedUntilMs.clear();
    }

    public void grant(Player player, boolean notify) {
        if (player == null) {
            return;
        }
        long until = System.currentTimeMillis() + config.getSpawnProtectionSeconds() * 1000L;
        protectedUntilMs.put(player.getUniqueId(), until);
        if (notify) {
            MessageUtil.send(player, MessageUtil.format(
                    config.getSpawnProtectionGranted(),
                    "seconds", String.valueOf(config.getSpawnProtectionSeconds())
            ));
        }
    }

    public void grantAllOnline(boolean notify) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            grant(player, notify);
        }
    }

    public boolean isProtected(Player player) {
        if (player == null) {
            return false;
        }
        Long until = protectedUntilMs.get(player.getUniqueId());
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            protectedUntilMs.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        if (protectedUntilMs.remove(player.getUniqueId()) != null) {
            MessageUtil.send(player, config.getSpawnProtectionEnded());
        }
    }

    public void clearQuiet(Player player) {
        if (player != null) {
            protectedUntilMs.remove(player.getUniqueId());
        }
    }
}
