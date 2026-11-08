package com.frostsmp.combat.service;

import com.frostsmp.combat.config.PluginConfig;
import com.frostsmp.combat.storage.KillStreakStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KillStreakService {

    public record Entry(UUID id, String name, int streak) {
    }

    private final PluginConfig config;
    private final KillStreakStorage storage;
    private final Map<UUID, Integer> current = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    public KillStreakService(PluginConfig config, KillStreakStorage storage) {
        this.config = config;
        this.storage = storage;
        for (KillStreakStorage.Record record : storage.all()) {
            current.put(record.id(), record.current());
            names.put(record.id(), record.name());
        }
    }

    public int current(UUID playerId) {
        return current.getOrDefault(playerId, 0);
    }

    public int best(UUID playerId) {
        return storage.best(playerId);
    }

    /** Increments killer streak after a kill; returns tokens to award. */
    public int registerKill(Player killer) {
        if (killer == null) {
            return 0;
        }
        UUID id = killer.getUniqueId();
        names.put(id, killer.getName());
        int streak = current.getOrDefault(id, 0) + 1;
        current.put(id, streak);
        storage.update(id, killer.getName(), streak, Math.max(streak, storage.best(id)));
        return config.tokensForStreak(streak);
    }

    public void reset(UUID playerId, String name) {
        current.put(playerId, 0);
        if (name != null) {
            names.put(playerId, name);
        }
        storage.update(playerId, names.getOrDefault(playerId, name != null ? name : "Unknown"), 0, storage.best(playerId));
    }

    public void touchName(Player player) {
        names.put(player.getUniqueId(), player.getName());
        storage.touchName(player.getUniqueId(), player.getName());
    }

    public void save() {
        storage.save();
    }

    /** Clears all current/best killstreaks and persists. Returns how many records existed. */
    public int resetAll() {
        int before = current.size();
        current.clear();
        names.clear();
        storage.clear();
        return before;
    }

    public Entry top(int rank) {
        if (rank < 1) {
            return null;
        }
        List<Entry> sorted = leaderboard();
        if (rank > sorted.size()) {
            return null;
        }
        return sorted.get(rank - 1);
    }

    /** 1-based place on the current killstreak board, or 0 if not ranked. */
    public int place(UUID playerId) {
        if (playerId == null || current(playerId) <= 0) {
            return 0;
        }
        List<Entry> sorted = leaderboard();
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).id().equals(playerId)) {
                return i + 1;
            }
        }
        return 0;
    }

    public List<Entry> leaderboard() {
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : current.entrySet()) {
            int streak = e.getValue();
            if (streak <= 0) {
                continue;
            }
            String name = names.get(e.getKey());
            if (name == null) {
                Player online = Bukkit.getPlayer(e.getKey());
                name = online != null ? online.getName() : storage.name(e.getKey());
            }
            if (name == null || name.isBlank()) {
                name = "Unknown";
            }
            entries.add(new Entry(e.getKey(), name, streak));
        }
        entries.sort(Comparator
                .comparingInt(Entry::streak).reversed()
                .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }
}
