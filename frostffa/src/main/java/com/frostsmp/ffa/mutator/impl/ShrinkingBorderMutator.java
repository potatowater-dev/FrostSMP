package com.frostsmp.ffa.mutator.impl;

import com.frostsmp.ffa.arena.ArenaService;
import com.frostsmp.ffa.config.PluginConfig;
import com.frostsmp.ffa.mutator.Mutator;
import com.frostsmp.ffa.mutator.MutatorId;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ShrinkingBorderMutator implements Mutator {

    private final PluginConfig config;
    private final ArenaService arena;

    private World world;
    private Snapshot original;
    private long durationMs;

    public ShrinkingBorderMutator(JavaPlugin plugin, PluginConfig config, ArenaService arena) {
        this.config = config;
        this.arena = arena;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    @Override
    public String id() {
        return MutatorId.SHRINKING_BORDER.id();
    }

    @Override
    public String displayName() {
        return MutatorId.SHRINKING_BORDER.displayName();
    }

    @Override
    public void start() {
        Location center = arena.center();
        var min = arena.min();
        var max = arena.max();
        world = arena.world();
        if (center == null || min == null || max == null || world == null) {
            return;
        }
        if (durationMs <= 0) {
            durationMs = 30L * 60L * 1000L;
        }

        // Drop any leftover per-player fake borders
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setWorldBorder(null);
        }

        WorldBorder border = world.getWorldBorder();
        original = Snapshot.capture(border);

        double startSize = Math.max(max.x() - min.x(), max.z() - min.z()) + 2.0;
        double endSize = config.shrinkingFinalSize();
        long shrinkTicks = Math.max(1L, config.shrinkingMinutes() * 60L * 20L);

        border.setCenter(center.getX(), center.getZ());
        border.setDamageBuffer(config.shrinkingDamageBuffer());
        border.setDamageAmount(config.shrinkingDamage());
        border.setWarningDistance(Math.max(0, config.shrinkingWarningDistance()));
        border.setSize(startSize);
        if (startSize <= endSize) {
            border.setSize(endSize);
        } else {
            border.changeSize(endSize, shrinkTicks);
        }
    }

    @Override
    public void stop() {
        if (world != null && original != null) {
            original.restore(world.getWorldBorder());
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setWorldBorder(null);
        }
        world = null;
        original = null;
    }

    private record Snapshot(
            double centerX,
            double centerZ,
            double size,
            double damageAmount,
            double damageBuffer,
            int warningDistance,
            int warningTime
    ) {
        static Snapshot capture(WorldBorder border) {
            Location center = border.getCenter();
            return new Snapshot(
                    center.getX(),
                    center.getZ(),
                    border.getSize(),
                    border.getDamageAmount(),
                    border.getDamageBuffer(),
                    border.getWarningDistance(),
                    border.getWarningTimeTicks()
            );
        }

        void restore(WorldBorder border) {
            border.setCenter(centerX, centerZ);
            border.setSize(size);
            border.setDamageAmount(damageAmount);
            border.setDamageBuffer(damageBuffer);
            border.setWarningDistance(warningDistance);
            border.setWarningTimeTicks(warningTime);
        }
    }
}
