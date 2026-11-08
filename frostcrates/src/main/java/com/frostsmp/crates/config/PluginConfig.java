package com.frostsmp.crates.config;

import com.frostsmp.crates.FrostCratesPlugin;
import org.bukkit.Sound;

public final class PluginConfig {

    private final FrostCratesPlugin plugin;
    private String prefix;
    private String defaultNoKeyMessage;
    private String broadcastFormat;
    private boolean actionBarNoKey;
    private int chatInputTimeoutSeconds;
    private int claimDurationTicks;
    private int claimScrollDistance;
    private boolean claimAnimationEnabled;
    private boolean claimAnimationAllowSkipPermission;
    private boolean claimTitleEnabled;
    private boolean warnOnFullInventory;
    private int claimBatchMaxKeys;
    private Sound claimSuccessSound;
    private Sound claimDeniedSound;
    private float claimSuccessVolume;
    private float claimSuccessPitch;
    private float claimDeniedVolume;
    private float claimDeniedPitch;

    private double hologramLine1Height;
    private double hologramLine2Height;
    private String hologramGradientStart;
    private String hologramGradientEnd;
    private String hologramSubtitle;
    private double hologramLine3Height;
    private String hologramKeysFormat;
    private int hologramViewDistance;

    private int autoKeyCheckIntervalSeconds;
    private int autoKeyMaxCatchup;
    private boolean autoKeyNotifyOnline;

    public PluginConfig(FrostCratesPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        prefix = plugin.getConfig().getString("prefix", "<gray>[<aqua>FrostCrates<gray>] ");
        defaultNoKeyMessage = plugin.getConfig().getString("default-no-key-message", "<red>You don't have a key for this crate!</red>");
        broadcastFormat = plugin.getConfig().getString("broadcast-format", "<gold>{player}</gold> won <yellow>{item}</yellow> from <aqua>{crate}</aqua>!");
        actionBarNoKey = plugin.getConfig().getBoolean("action-bar-no-key", true);
        chatInputTimeoutSeconds = plugin.getConfig().getInt("chat-input-timeout-seconds", 30);
        claimDurationTicks = Math.max(20, plugin.getConfig().getInt("claim-animation.duration-ticks", 90));
        claimScrollDistance = plugin.getConfig().getInt("claim-animation.scroll-distance", 40);
        claimAnimationEnabled = plugin.getConfig().getBoolean("claim-animation.enabled", true);
        claimAnimationAllowSkipPermission = plugin.getConfig().getBoolean("claim-animation.allow-skip-permission", false);
        claimTitleEnabled = plugin.getConfig().getBoolean("claim-title-enabled", true);
        warnOnFullInventory = plugin.getConfig().getBoolean("warn-on-full-inventory", true);
        claimBatchMaxKeys = Math.max(1, plugin.getConfig().getInt("claim-batch.max-keys", 64));
        claimSuccessSound = parseSound(plugin.getConfig().getString("sounds.claim-success"), Sound.ENTITY_PLAYER_LEVELUP);
        claimDeniedSound = parseSound(plugin.getConfig().getString("sounds.claim-denied"), Sound.BLOCK_NOTE_BLOCK_BASS);
        claimSuccessVolume = (float) plugin.getConfig().getDouble("sounds.claim-success-volume", 0.9);
        claimSuccessPitch = (float) plugin.getConfig().getDouble("sounds.claim-success-pitch", 1.2);
        claimDeniedVolume = (float) plugin.getConfig().getDouble("sounds.claim-denied-volume", 0.8);
        claimDeniedPitch = (float) plugin.getConfig().getDouble("sounds.claim-denied-pitch", 0.5);
        hologramLine1Height = plugin.getConfig().getDouble("hologram.line1-height", 1.85);
        hologramLine2Height = plugin.getConfig().getDouble("hologram.line2-height", 1.55);
        hologramGradientStart = plugin.getConfig().getString("hologram.gradient-start", "#55FFFF");
        hologramGradientEnd = plugin.getConfig().getString("hologram.gradient-end", "#FF55FF");
        hologramSubtitle = plugin.getConfig().getString(
                "hologram.subtitle",
                "<gray>✦ Right-click · Sneak+right-click opens all ✦"
        );
        hologramLine3Height = plugin.getConfig().getDouble("hologram.line3-height", 1.25);
        hologramKeysFormat = plugin.getConfig().getString("hologram.keys-format", "<gray>Your keys: <yellow>{keys}");
        hologramViewDistance = plugin.getConfig().getInt("hologram.view-distance", 48);
        autoKeyCheckIntervalSeconds = plugin.getConfig().getInt("auto-key-grants.check-interval-seconds", 60);
        autoKeyMaxCatchup = plugin.getConfig().getInt("auto-key-grants.max-catchup", 7);
        autoKeyNotifyOnline = plugin.getConfig().getBoolean("auto-key-grants.notify-online", true);
    }

    private Sound parseSound(String name, Sound fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid sound: " + name);
            return fallback;
        }
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDefaultNoKeyMessage() {
        return defaultNoKeyMessage;
    }

    public String getBroadcastFormat() {
        return broadcastFormat;
    }

    public boolean isActionBarNoKey() {
        return actionBarNoKey;
    }

    public int getChatInputTimeoutSeconds() {
        return chatInputTimeoutSeconds;
    }

    public int getClaimDurationTicks() {
        return claimDurationTicks;
    }

    public int getClaimScrollDistance() {
        return claimScrollDistance;
    }

    public boolean isClaimAnimationEnabled() {
        return claimAnimationEnabled;
    }

    public boolean isClaimAnimationAllowSkipPermission() {
        return claimAnimationAllowSkipPermission;
    }

    public boolean isClaimTitleEnabled() {
        return claimTitleEnabled;
    }

    public boolean isWarnOnFullInventory() {
        return warnOnFullInventory;
    }

    public int getClaimBatchMaxKeys() {
        return claimBatchMaxKeys;
    }

    public Sound getClaimSuccessSound() {
        return claimSuccessSound;
    }

    public Sound getClaimDeniedSound() {
        return claimDeniedSound;
    }

    public float getClaimSuccessVolume() {
        return claimSuccessVolume;
    }

    public float getClaimSuccessPitch() {
        return claimSuccessPitch;
    }

    public float getClaimDeniedVolume() {
        return claimDeniedVolume;
    }

    public float getClaimDeniedPitch() {
        return claimDeniedPitch;
    }

    public double getHologramLine1Height() {
        return hologramLine1Height;
    }

    public double getHologramLine2Height() {
        return hologramLine2Height;
    }

    public String getHologramGradientStart() {
        return hologramGradientStart;
    }

    public String getHologramGradientEnd() {
        return hologramGradientEnd;
    }

    public String getHologramSubtitle() {
        return hologramSubtitle;
    }

    public double getHologramLine3Height() {
        return hologramLine3Height;
    }

    public String getHologramKeysFormat() {
        return hologramKeysFormat;
    }

    public int getHologramViewDistance() {
        return hologramViewDistance;
    }

    public int getAutoKeyCheckIntervalSeconds() {
        return autoKeyCheckIntervalSeconds;
    }

    public int getAutoKeyMaxCatchup() {
        return autoKeyMaxCatchup;
    }

    public boolean isAutoKeyNotifyOnline() {
        return autoKeyNotifyOnline;
    }
}
