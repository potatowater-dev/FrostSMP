package com.frostsmp.colosseum.model;

import org.bukkit.Location;
import org.bukkit.World;

public final class CuboidRegion {

    private final World world;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public CuboidRegion(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null
                || !a.getWorld().equals(b.getWorld())) {
            throw new IllegalArgumentException("Invalid region corners");
        }
        this.world = a.getWorld();
        this.minX = Math.min(a.getBlockX(), b.getBlockX());
        this.minY = Math.min(a.getBlockY(), b.getBlockY());
        this.minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        this.maxX = Math.max(a.getBlockX(), b.getBlockX());
        this.maxY = Math.max(a.getBlockY(), b.getBlockY());
        this.maxZ = Math.max(a.getBlockZ(), b.getBlockZ());
    }

    public World getWorld() {
        return world;
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null || !location.getWorld().equals(world)) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public Location center() {
        return new Location(world, (minX + maxX) / 2.0 + 0.5, minY + 1.0, (minZ + maxZ) / 2.0 + 0.5);
    }

    public Location getMinCorner() {
        return new Location(world, minX, minY, minZ);
    }

    public Location getMaxCorner() {
        return new Location(world, maxX, maxY, maxZ);
    }

    public String format() {
        return world.getName() + " " + minX + "," + minY + "," + minZ + " → " + maxX + "," + maxY + "," + maxZ;
    }
}
