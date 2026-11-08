package com.frostsmp.pearls.tracker;

import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PearlCatchTracker {

    public record TrackedPearl(UUID throwerId, long expiresAtTick) {
        public boolean isExpired(long currentTick) {
            return currentTick >= expiresAtTick;
        }
    }

    private final Map<UUID, TrackedPearl> tracked = new ConcurrentHashMap<>();
    private long currentTick;

    public void tick() {
        currentTick++;
        Iterator<Map.Entry<UUID, TrackedPearl>> iterator = tracked.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TrackedPearl> entry = iterator.next();
            if (entry.getValue().isExpired(currentTick)) {
                iterator.remove();
            }
        }
    }

    public void track(EnderPearl pearl, Player thrower, int windowTicks) {
        tracked.put(pearl.getUniqueId(), new TrackedPearl(
                thrower.getUniqueId(),
                currentTick + windowTicks
        ));
    }

    public TrackedPearl get(EnderPearl pearl) {
        TrackedPearl trackedPearl = tracked.get(pearl.getUniqueId());
        if (trackedPearl == null || trackedPearl.isExpired(currentTick)) {
            tracked.remove(pearl.getUniqueId());
            return null;
        }
        return trackedPearl;
    }

    public void untrack(EnderPearl pearl) {
        tracked.remove(pearl.getUniqueId());
    }
}
