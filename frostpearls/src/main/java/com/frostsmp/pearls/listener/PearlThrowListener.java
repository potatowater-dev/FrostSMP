package com.frostsmp.pearls.listener;

import com.frostsmp.pearls.config.PluginConfig;
import com.frostsmp.pearls.physics.PearlVelocityCalculator;
import com.frostsmp.pearls.tracker.PearlCatchTracker;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

public final class PearlThrowListener implements Listener {

    private final PluginConfig config;
    private final PearlVelocityCalculator calculator;
    private final PearlCatchTracker tracker;

    public PearlThrowListener(
            PluginConfig config,
            PearlVelocityCalculator calculator,
            PearlCatchTracker tracker
    ) {
        this.config = config;
        this.calculator = calculator;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPearlLaunch(ProjectileLaunchEvent event) {
        if (!config.isEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof EnderPearl pearl)) {
            return;
        }
        if (!(pearl.getShooter() instanceof Player player)) {
            return;
        }
        if (!player.hasPermission("frostsmp.pearls.use")) {
            return;
        }

        float pitch = player.getLocation().getPitch();
        if (pitch > config.getMinPitch()) {
            return;
        }

        Vector velocity = calculator.calculateStraightUpVelocity(player);
        pearl.setVelocity(velocity);
        tracker.track(pearl, player, config.getComboWindowTicks());

        if (config.isDebug()) {
            player.sendMessage("FrostPearls: straight-up Y=" + String.format("%.2f", velocity.getY()));
        }
    }
}
