package com.frostsmp.pearls.physics;

import com.frostsmp.pearls.config.PluginConfig;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class PearlVelocityCalculator {

    private final PluginConfig config;

    public PearlVelocityCalculator(PluginConfig config) {
        this.config = config;
    }

    public Vector calculateStraightUpVelocity(Player player) {
        Vector playerVel = player.getVelocity();
        double horizontalSpeed = Math.hypot(playerVel.getX(), playerVel.getZ());

        double verticalSpeed = config.getBasePearlSpeed()
                + playerVel.getY()
                + horizontalSpeed * config.getSprintMomentumFactor();

        verticalSpeed = Math.max(config.getMinVerticalSpeed(),
                Math.min(config.getMaxVerticalSpeed(), verticalSpeed));

        return new Vector(0, verticalSpeed, 0);
    }
}
