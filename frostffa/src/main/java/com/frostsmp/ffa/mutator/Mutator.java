package com.frostsmp.ffa.mutator;

import org.bukkit.entity.Player;

public interface Mutator {

    String id();

    String displayName();

    void start();

    void stop();

    default void tick() {
    }

    default void onEnter(Player player) {
    }

    default void onExit(Player player) {
    }
}
