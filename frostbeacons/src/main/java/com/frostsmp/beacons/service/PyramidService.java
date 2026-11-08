package com.frostsmp.beacons.service;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class PyramidService {

    private static final Material[] BASE = {
            Material.IRON_BLOCK,
            Material.GOLD_BLOCK,
            Material.EMERALD_BLOCK,
            Material.DIAMOND_BLOCK,
            Material.NETHERITE_BLOCK
    };

    public int calculateTier(Location beaconLocation) {
        World world = beaconLocation.getWorld();
        if (world == null) {
            return 0;
        }
        int x = beaconLocation.getBlockX();
        int y = beaconLocation.getBlockY();
        int z = beaconLocation.getBlockZ();
        int tier = 0;
        for (int level = 1; level <= 4; level++) {
            if (!isCompleteLayer(world, x, y - level, z, level)) {
                break;
            }
            tier = level;
        }
        return tier;
    }

    private static boolean isCompleteLayer(World world, int beaconX, int layerY, int beaconZ, int level) {
        for (int dx = -level; dx <= level; dx++) {
            for (int dz = -level; dz <= level; dz++) {
                if (dx == 0 && dz == 0 && level == 0) {
                    continue;
                }
                Block block = world.getBlockAt(beaconX + dx, layerY, beaconZ + dz);
                if (!isBeaconBase(block.getType())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isBeaconBase(Material material) {
        for (Material allowed : BASE) {
            if (allowed == material) {
                return true;
            }
        }
        return false;
    }
}
