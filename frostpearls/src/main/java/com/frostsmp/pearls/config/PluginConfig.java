package com.frostsmp.pearls.config;

import com.frostsmp.pearls.FrostPearlsPlugin;

public final class PluginConfig {

    private final FrostPearlsPlugin plugin;
    private boolean enabled;
    private float minPitch;
    private double basePearlSpeed;
    private double sprintMomentumFactor;
    private double minVerticalSpeed;
    private double maxVerticalSpeed;
    private boolean windChargeEnabled;
    private int comboWindowTicks;
    private double windChargeBoostY;
    private boolean requireSamePlayer;
    private boolean debug;
    private boolean pearlCooldownEnabled;
    private long pearlCooldownMs;
    private String pearlCooldownMessage;

    public PluginConfig(FrostPearlsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        var config = plugin.getConfig();
        enabled = config.getBoolean("enabled", true);
        minPitch = (float) config.getDouble("min-pitch", -80);
        basePearlSpeed = config.getDouble("pearl.base-speed", 1.5);
        sprintMomentumFactor = config.getDouble("pearl.sprint-momentum-factor", 0.35);
        minVerticalSpeed = config.getDouble("pearl.min-vertical-speed", 0.8);
        maxVerticalSpeed = config.getDouble("pearl.max-vertical-speed", 3.5);
        windChargeEnabled = config.getBoolean("wind-charge.enabled", true);
        comboWindowTicks = config.getInt("wind-charge.combo-window-ticks", 20);
        windChargeBoostY = config.getDouble("wind-charge.boost-y", 1.8);
        requireSamePlayer = config.getBoolean("wind-charge.require-same-player", true);
        debug = config.getBoolean("debug", false);
        pearlCooldownEnabled = config.getBoolean("cooldown.enabled", true);
        pearlCooldownMs = Math.max(0L, config.getLong("cooldown.seconds", 10L) * 1000L);
        pearlCooldownMessage = config.getString("messages.pearl-cooldown",
                "<red>Pearl on cooldown: <white>{seconds}s</white></red>");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public float getMinPitch() {
        return minPitch;
    }

    public double getBasePearlSpeed() {
        return basePearlSpeed;
    }

    public double getSprintMomentumFactor() {
        return sprintMomentumFactor;
    }

    public double getMinVerticalSpeed() {
        return minVerticalSpeed;
    }

    public double getMaxVerticalSpeed() {
        return maxVerticalSpeed;
    }

    public boolean isWindChargeEnabled() {
        return windChargeEnabled;
    }

    public int getComboWindowTicks() {
        return comboWindowTicks;
    }

    public double getWindChargeBoostY() {
        return windChargeBoostY;
    }

    public boolean isRequireSamePlayer() {
        return requireSamePlayer;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isPearlCooldownEnabled() {
        return pearlCooldownEnabled;
    }

    public long getPearlCooldownMs() {
        return pearlCooldownMs;
    }

    public String getPearlCooldownMessage() {
        return pearlCooldownMessage;
    }
}
