package com.frostsmp.crates.service;

import com.frostsmp.crates.config.KeyStorage;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public final class KeyService {

    private final KeyStorage storage;

    public KeyService(KeyStorage storage) {
        this.storage = storage;
    }

    public int get(Player player, String crateId) {
        return storage.get(player.getUniqueId(), crateId);
    }

    public int get(UUID playerId, String crateId) {
        return storage.get(playerId, crateId);
    }

    public void give(Player player, String crateId, int amount) {
        give(player.getUniqueId(), crateId, amount);
    }

    public void give(UUID playerId, String crateId, int amount) {
        int current = get(playerId, crateId);
        storage.set(playerId, crateId, current + amount, false);
    }

    public void flush() {
        storage.save();
    }

    public void take(Player player, String crateId, int amount) {
        int current = get(player, crateId);
        storage.set(player.getUniqueId(), crateId, Math.max(0, current - amount));
    }

    public boolean tryConsume(Player player, String crateId) {
        return tryConsume(player, crateId, 1);
    }

    public boolean tryConsume(Player player, String crateId, int amount) {
        if (amount < 1) {
            return false;
        }
        int current = get(player, crateId);
        if (current < amount) {
            return false;
        }
        storage.set(player.getUniqueId(), crateId, current - amount);
        return true;
    }

    public Map<String, Integer> getAll(Player player) {
        return storage.getAll(player.getUniqueId());
    }
}
