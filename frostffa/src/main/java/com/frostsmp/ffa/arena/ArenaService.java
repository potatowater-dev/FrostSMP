package com.frostsmp.ffa.arena;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArenaService {

    private final JavaPlugin plugin;
    private String worldName;
    private String regionId;

    public ArenaService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        worldName = plugin.getConfig().getString("arena.world", "world");
        regionId = plugin.getConfig().getString("arena.region", "ffa");
    }

    public String worldName() {
        return worldName;
    }

    public String regionId() {
        return regionId;
    }

    public World world() {
        return Bukkit.getWorld(worldName);
    }

    public ProtectedRegion region() {
        World world = world();
        if (world == null) {
            return null;
        }
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));
        if (manager == null) {
            return null;
        }
        return manager.getRegion(regionId);
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }
        ProtectedRegion region = region();
        if (region == null) {
            return false;
        }
        return region.contains(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }

    public boolean contains(Player player) {
        return player != null && contains(player.getLocation());
    }

    public boolean containsBlock(int x, int y, int z) {
        ProtectedRegion region = region();
        return region != null && region.contains(BlockVector3.at(x, y, z));
    }

    public BlockVector3 min() {
        ProtectedRegion region = region();
        return region == null ? null : region.getMinimumPoint();
    }

    public BlockVector3 max() {
        ProtectedRegion region = region();
        return region == null ? null : region.getMaximumPoint();
    }

    public Location center() {
        ProtectedRegion region = region();
        World world = world();
        if (region == null || world == null) {
            return null;
        }
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        double x = (min.x() + max.x() + 1) / 2.0;
        double y = (min.y() + max.y() + 1) / 2.0;
        double z = (min.z() + max.z() + 1) / 2.0;
        return new Location(world, x, y, z);
    }
}
