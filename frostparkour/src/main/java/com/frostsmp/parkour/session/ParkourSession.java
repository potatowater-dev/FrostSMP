package com.frostsmp.parkour.session;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.UUID;

public final class ParkourSession {

    private final UUID playerId;
    private final String courseId;
    private long startTimeMs;
    private int currentCheckpointIndex = -1; // -1 = at start
    private Location lastCheckpointLocation;

    // Saved state to restore on quit/finish
    private final ItemStack[] savedStorage;
    private final ItemStack[] savedArmor;
    private final ItemStack[] savedExtra;
    private final Location savedLocation;
    private final GameMode savedGameMode;
    private final boolean savedAllowFlight;
    private final boolean savedFlying;
    private final int savedFoodLevel;
    private final float savedSaturation;
    private final double savedHealth;
    private final Collection<PotionEffect> savedEffects;

    private BukkitTask timerTask;

    public ParkourSession(Player player, String courseId, Location startLocation) {
        this.playerId = player.getUniqueId();
        this.courseId = courseId;
        this.startTimeMs = System.currentTimeMillis();
        this.lastCheckpointLocation = startLocation.clone();

        // Snapshot player state
        this.savedStorage = cloneItemArray(player.getInventory().getStorageContents());
        this.savedArmor = cloneItemArray(player.getInventory().getArmorContents());
        this.savedExtra = cloneItemArray(player.getInventory().getExtraContents());
        this.savedLocation = player.getLocation().clone();
        this.savedGameMode = player.getGameMode();
        this.savedAllowFlight = player.getAllowFlight();
        this.savedFlying = player.isFlying();
        this.savedFoodLevel = player.getFoodLevel();
        this.savedSaturation = player.getSaturation();
        this.savedHealth = player.getHealth();
        this.savedEffects = player.getActivePotionEffects();
    }

    private static ItemStack[] cloneItemArray(ItemStack[] source) {
        if (source == null) {
            return new ItemStack[0];
        }
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] != null ? source[i].clone() : null;
        }
        return copy;
    }

    public void restorePlayer(Player player) {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        player.getInventory().clear();
        player.getInventory().setStorageContents(savedStorage);
        player.getInventory().setArmorContents(savedArmor);
        player.getInventory().setExtraContents(savedExtra);

        player.setGameMode(savedGameMode);
        player.setAllowFlight(savedAllowFlight);
        player.setFlying(savedFlying);
        player.setFoodLevel(savedFoodLevel);
        player.setSaturation(savedSaturation);
        try {
            player.setHealth(Math.min(player.getMaxHealth(), savedHealth));
        } catch (Exception ignored) {
        }

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : savedEffects) {
            player.addPotionEffect(effect);
        }

        player.teleport(savedLocation);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getCourseId() {
        return courseId;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public void resetTimer() {
        this.startTimeMs = System.currentTimeMillis();
    }

    public long getElapsedTimeMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    public int getCurrentCheckpointIndex() {
        return currentCheckpointIndex;
    }

    public void setCurrentCheckpointIndex(int currentCheckpointIndex) {
        this.currentCheckpointIndex = currentCheckpointIndex;
    }

    public Location getLastCheckpointLocation() {
        return lastCheckpointLocation;
    }

    public void setLastCheckpointLocation(Location lastCheckpointLocation) {
        this.lastCheckpointLocation = lastCheckpointLocation != null ? lastCheckpointLocation.clone() : null;
    }

    public BukkitTask getTimerTask() {
        return timerTask;
    }

    public void setTimerTask(BukkitTask timerTask) {
        this.timerTask = timerTask;
    }
}
