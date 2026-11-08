package com.frostsmp.crates.service;

import com.frostsmp.crates.FrostCratesPlugin;
import com.frostsmp.crates.model.AutoKeySettings;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PermissionLookupService {

    private final FrostCratesPlugin plugin;
    private LuckPerms luckPerms;

    public PermissionLookupService(FrostCratesPlugin plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                luckPerms = LuckPermsProvider.get();
                plugin.getLogger().info("LuckPerms detected — auto-key grants use rank/group checks.");
            } catch (IllegalStateException exception) {
                plugin.getLogger().warning("LuckPerms is installed but API is unavailable. Auto-key rank checks disabled.");
            }
        } else {
            plugin.getLogger().warning("LuckPerms not found. Auto-key grants require LuckPerms for rank checks.");
        }
    }

    public boolean isLuckPermsAvailable() {
        return luckPerms != null;
    }

    public Set<UUID> getAllTrackedPlayerIds() {
        Set<UUID> ids = new HashSet<>();
        if (luckPerms != null) {
            ids.addAll(loadAllUserIds());
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            ids.add(player.getUniqueId());
        }
        return ids;
    }

    public Set<UUID> findEligiblePlayers(AutoKeySettings autoKey) {
        if (autoKey.usesGroup()) {
            return findPlayersInGroup(autoKey.getGroup());
        }
        if (autoKey.getPermission() != null && !autoKey.getPermission().isBlank()) {
            return findPlayersWithPermission(autoKey.getPermission());
        }
        return Set.of();
    }

    public Set<UUID> findPlayersInGroup(String groupName) {
        Set<UUID> matches = new HashSet<>();
        if (groupName == null || groupName.isBlank()) {
            return matches;
        }
        if (luckPerms == null) {
            plugin.getLogger().warning("Cannot grant auto-keys for group '" + groupName + "' without LuckPerms.");
            return matches;
        }
        Set<UUID> users = loadAllUserIds();
        for (UUID uuid : users) {
            if (isInGroup(uuid, groupName)) {
                matches.add(uuid);
            }
        }
        return matches;
    }

    public Set<UUID> findPlayersWithPermission(String permission) {
        Set<UUID> matches = new HashSet<>();
        if (permission == null || permission.isBlank()) {
            return matches;
        }
        if (luckPerms != null) {
            for (UUID uuid : loadAllUserIds()) {
                if (hasPermission(uuid, permission)) {
                    matches.add(uuid);
                }
            }
            return matches;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                matches.add(player.getUniqueId());
            }
        }
        return matches;
    }

    public boolean isInGroup(UUID playerId, String groupName) {
        if (luckPerms == null || groupName == null || groupName.isBlank()) {
            return false;
        }
        User user = getUser(playerId);
        if (user == null) {
            return false;
        }
        QueryOptions options = QueryOptions.defaultContextualOptions();
        if (user.getPrimaryGroup().equalsIgnoreCase(groupName)) {
            return true;
        }
        return user.getInheritedGroups(options).stream()
                .anyMatch(group -> group.getName().equalsIgnoreCase(groupName));
    }

    public boolean hasPermission(UUID playerId, String permission) {
        if (permission == null || permission.isBlank()) {
            return false;
        }
        if (luckPerms != null) {
            User user = getUser(playerId);
            if (user == null) {
                return false;
            }
            return user.getCachedData().getPermissionData(QueryOptions.defaultContextualOptions())
                    .checkPermission(permission)
                    .asBoolean();
        }
        Player online = Bukkit.getPlayer(playerId);
        return online != null && online.hasPermission(permission);
    }

    private Set<UUID> loadAllUserIds() {
        try {
            return luckPerms.getUserManager().getUniqueUsers().join();
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to load LuckPerms users: " + exception.getMessage());
            return Set.of();
        }
    }

    private User getUser(UUID playerId) {
        User user = luckPerms.getUserManager().getUser(playerId);
        if (user != null) {
            return user;
        }
        try {
            return luckPerms.getUserManager().loadUser(playerId).join();
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to load LuckPerms user " + playerId + ": " + exception.getMessage());
            return null;
        }
    }
}
