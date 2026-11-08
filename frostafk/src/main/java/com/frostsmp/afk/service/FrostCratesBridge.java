package com.frostsmp.afk.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Gives FrostCrates keys without a hard classloader dependency.
 * Direct casts fail across Paper plugin classloaders; reflection + console fallback work.
 */
public final class FrostCratesBridge {

    private final JavaPlugin plugin;

    public FrostCratesBridge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean giveKey(Player player, String crateId, int amount) {
        if (player == null || crateId == null || crateId.isBlank() || amount < 1) {
            return false;
        }
        String id = crateId.toLowerCase(Locale.ROOT);
        if (giveViaReflection(player, id, amount)) {
            return true;
        }
        return giveViaCommand(player, id, amount);
    }

    public boolean isAvailable() {
        Plugin crates = Bukkit.getPluginManager().getPlugin("FrostCrates");
        return crates != null && crates.isEnabled();
    }

    private boolean giveViaReflection(Player player, String crateId, int amount) {
        Plugin crates = Bukkit.getPluginManager().getPlugin("FrostCrates");
        if (crates == null || !crates.isEnabled()) {
            return false;
        }
        try {
            Method getKeyService = crates.getClass().getMethod("getKeyService");
            Object keyService = getKeyService.invoke(crates);
            if (keyService == null) {
                return false;
            }
            Method give = keyService.getClass().getMethod("give", Player.class, String.class, int.class);
            give.invoke(keyService, player, crateId, amount);
            return true;
        } catch (NoSuchMethodException exception) {
            plugin.getLogger().warning("FrostCrates is outdated — missing getKeyService(). Update FrostCrates.jar.");
            return false;
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to give AFK key via FrostCrates API", exception);
            return false;
        }
    }

    private boolean giveViaCommand(Player player, String crateId, int amount) {
        if (!isAvailable()) {
            return false;
        }
        String command = "crate keys give " + player.getName() + " " + crateId + " " + amount;
        try {
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to give AFK key via /crate command", exception);
            return false;
        }
    }
}
