package com.frostsmp.juggernaut.service;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Total damage dealt to the juggernauts, summed across all of them.
 * Keyed by UUID so a player keeps their score across disconnects.
 */
public final class DamageTracker {

    public record Entry(UUID playerId, String name, double damage) {
    }

    private final Map<UUID, Double> damageByPlayer = new HashMap<>();
    private final Map<UUID, Long> lastHitAt = new HashMap<>();
    private final Map<UUID, String> nameByPlayer = new HashMap<>();

    public void addDamage(Player damager, double amount) {
        if (damager == null || amount <= 0) {
            return;
        }
        UUID id = damager.getUniqueId();
        damageByPlayer.merge(id, amount, Double::sum);
        lastHitAt.put(id, System.currentTimeMillis());
        nameByPlayer.put(id, damager.getName());
    }

    public double getDamage(UUID playerId) {
        if (playerId == null) {
            return 0.0;
        }
        return damageByPlayer.getOrDefault(playerId, 0.0);
    }

    /**
     * Name captured at hit time, so offline players still resolve without a blocking lookup.
     */
    public String getName(UUID playerId) {
        return playerId == null ? null : nameByPlayer.get(playerId);
    }

    /**
     * Highest total damage; ties broken by whoever reached it first.
     */
    public Entry getTop() {
        UUID best = null;
        double bestDamage = 0.0;
        long bestHitAt = Long.MAX_VALUE;
        for (Map.Entry<UUID, Double> entry : damageByPlayer.entrySet()) {
            double damage = entry.getValue();
            long hitAt = lastHitAt.getOrDefault(entry.getKey(), Long.MAX_VALUE);
            if (best == null
                    || damage > bestDamage
                    || (damage == bestDamage && hitAt < bestHitAt)) {
                best = entry.getKey();
                bestDamage = damage;
                bestHitAt = hitAt;
            }
        }
        return best == null ? null : new Entry(best, nameByPlayer.get(best), bestDamage);
    }

    public List<Entry> getLeaderboard(int limit) {
        List<Entry> entries = new ArrayList<>(damageByPlayer.size());
        for (Map.Entry<UUID, Double> entry : damageByPlayer.entrySet()) {
            entries.add(new Entry(entry.getKey(), nameByPlayer.get(entry.getKey()), entry.getValue()));
        }
        entries.sort(Comparator
                .comparingDouble(Entry::damage).reversed()
                .thenComparingLong(e -> lastHitAt.getOrDefault(e.playerId(), Long.MAX_VALUE)));
        if (limit > 0 && entries.size() > limit) {
            return entries.subList(0, limit);
        }
        return entries;
    }

    /**
     * 1-based placement, or 0 when the player has not dealt damage.
     */
    public int getRank(UUID playerId) {
        if (playerId == null || !damageByPlayer.containsKey(playerId)) {
            return 0;
        }
        List<Entry> leaderboard = getLeaderboard(0);
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).playerId().equals(playerId)) {
                return i + 1;
            }
        }
        return 0;
    }

    public boolean isEmpty() {
        return damageByPlayer.isEmpty();
    }

    public void clear() {
        damageByPlayer.clear();
        lastHitAt.clear();
        nameByPlayer.clear();
    }
}
