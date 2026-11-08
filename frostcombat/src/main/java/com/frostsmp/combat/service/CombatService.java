package com.frostsmp.combat.service;

import com.frostsmp.combat.config.PluginConfig;
import com.frostsmp.combat.display.CombatActionBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicLong;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatService {

    private final PluginConfig config;
    private final CombatActionBar actionBar;
    private final Map<UUID, Long> combatEndAt = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastHitter = new ConcurrentHashMap<>();
    private final Set<UUID> combatLogging = ConcurrentHashMap.newKeySet();
    private final AtomicLong combatLogGraceUntil = new AtomicLong(0L);
    private int animationFrame;

    public CombatService(PluginConfig config, CombatActionBar actionBar) {
        this.config = config;
        this.actionBar = actionBar;
    }

    public void tag(Player player) {
        if (player.hasPermission("frostsmp.combat.bypass")) {
            return;
        }
        combatEndAt.put(player.getUniqueId(), System.currentTimeMillis() + config.getCombatDurationMs());
    }

    public void tag(Player player, Player hitter) {
        tag(player);
        if (hitter != null && !hitter.equals(player)) {
            lastHitter.put(player.getUniqueId(), hitter.getUniqueId());
        }
    }

    public Player lastHitter(Player player) {
        UUID id = lastHitter.get(player.getUniqueId());
        return id == null ? null : Bukkit.getPlayer(id);
    }

    public void beginCombatLog(UUID playerId) {
        combatLogging.add(playerId);
    }

    public void endCombatLog(UUID playerId) {
        combatLogging.remove(playerId);
    }

    public boolean isCombatLogging(UUID playerId) {
        return combatLogging.contains(playerId);
    }

    /** Refreshes the combat timer only if the player is already tagged. */
    public void extendCombat(Player player) {
        if (player.hasPermission("frostsmp.combat.bypass")) {
            return;
        }
        Long endAt = combatEndAt.get(player.getUniqueId());
        if (endAt == null || System.currentTimeMillis() >= endAt) {
            return;
        }
        combatEndAt.put(player.getUniqueId(), System.currentTimeMillis() + config.getCombatDurationMs());
    }

    public boolean isInCombat(Player player) {
        if (player.hasPermission("frostsmp.combat.bypass")) {
            return false;
        }
        Long endAt = combatEndAt.get(player.getUniqueId());
        if (endAt == null) {
            return false;
        }
        if (System.currentTimeMillis() >= endAt) {
            combatEndAt.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public void clear(Player player) {
        UUID id = player.getUniqueId();
        combatEndAt.remove(id);
        lastHitter.remove(id);
        combatLogging.remove(id);
        if (player.isOnline()) {
            player.sendActionBar(Component.empty());
        }
    }

    public void enableCombatLogGrace(long durationMs) {
        if (durationMs <= 0) {
            return;
        }
        long until = System.currentTimeMillis() + durationMs;
        combatLogGraceUntil.updateAndGet(current -> Math.max(current, until));
    }

    public void disableCombatLogGrace() {
        combatLogGraceUntil.set(0L);
    }

    public boolean isCombatLogExempt() {
        return Bukkit.isStopping() || System.currentTimeMillis() < combatLogGraceUntil.get();
    }

    public void tick() {
        animationFrame++;
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, Long>> iterator = combatEndAt.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (now >= entry.getValue()) {
                iterator.remove();
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    player.sendActionBar(Component.empty());
                }
                continue;
            }
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline() && !player.hasPermission("frostsmp.combat.bypass")) {
                long remainingMs = entry.getValue() - now;
                int secondsRemaining = (int) Math.ceil(remainingMs / 1000.0);
                player.sendActionBar(actionBar.build(animationFrame, secondsRemaining));
            }
        }
    }
}
