package com.frostsmp.ffa.arena;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class MembershipCache {

    private final ArenaService arena;
    private final Set<UUID> inside = new HashSet<>();
    private BiConsumer<Player, Boolean> listener = (player, entered) -> {
    };

    public MembershipCache(ArenaService arena) {
        this.arena = arena;
    }

    public void setListener(BiConsumer<Player, Boolean> listener) {
        this.listener = listener != null ? listener : (player, entered) -> {
        };
    }

    public boolean isInArena(Player player) {
        return player != null && inside.contains(player.getUniqueId());
    }

    public Set<UUID> members() {
        return Set.copyOf(inside);
    }

    public void refresh(Player player) {
        if (player == null || !player.isOnline()) {
            remove(player);
            return;
        }
        boolean now = arena.contains(player);
        boolean was = inside.contains(player.getUniqueId());
        if (now && !was) {
            inside.add(player.getUniqueId());
            listener.accept(player, true);
        } else if (!now && was) {
            inside.remove(player.getUniqueId());
            listener.accept(player, false);
        }
    }

    public void remove(Player player) {
        if (player == null) {
            return;
        }
        if (inside.remove(player.getUniqueId())) {
            listener.accept(player, false);
        }
    }

    public void scanOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public void clearSilent() {
        inside.clear();
    }
}
