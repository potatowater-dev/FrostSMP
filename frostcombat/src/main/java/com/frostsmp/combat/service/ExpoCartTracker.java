package com.frostsmp.combat.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ExpoCartTracker {

    private final Set<UUID> projectileTagged = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Boolean> explosionTypes = new ConcurrentHashMap<>();

    public void tagExpo(UUID minecartId) {
        projectileTagged.add(minecartId);
    }

    public void classifyExplosion(UUID minecartId, boolean expo) {
        explosionTypes.put(minecartId, expo);
    }

    public Boolean consumeExplosionType(UUID minecartId) {
        return explosionTypes.remove(minecartId);
    }

    public boolean wasProjectileTagged(UUID minecartId) {
        return projectileTagged.remove(minecartId);
    }

    public void clear(UUID minecartId) {
        projectileTagged.remove(minecartId);
        explosionTypes.remove(minecartId);
    }
}
