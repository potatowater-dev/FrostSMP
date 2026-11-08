package com.frostsmp.pearls.cooldown;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PearlCooldownManager {

    private final Map<UUID, Long> expiry = new ConcurrentHashMap<>();

    public boolean isOnCooldown(Player player) {
        Long until = expiry.get(player.getUniqueId());
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            expiry.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public int getRemainingSeconds(Player player) {
        Long until = expiry.get(player.getUniqueId());
        if (until == null) {
            return 0;
        }
        long remainingMs = until - System.currentTimeMillis();
        if (remainingMs <= 0) {
            expiry.remove(player.getUniqueId());
            return 0;
        }
        return (int) Math.ceil(remainingMs / 1000.0);
    }

    public void setCooldown(Player player, long durationMs) {
        expiry.put(player.getUniqueId(), System.currentTimeMillis() + durationMs);
    }
}
