package com.frostsmp.beacons.service;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class BeaconRegistry {

    private final Map<String, Location> beacons = new ConcurrentHashMap<>();

    public void register(Location location) {
        if (location.getWorld() == null) {
            return;
        }
        beacons.put(key(location), location.clone());
    }

    public void unregister(Location location) {
        if (location.getWorld() == null) {
            return;
        }
        beacons.remove(key(location));
    }

    public void scanChunk(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (state.getType() == Material.BEACON) {
                register(state.getLocation());
            }
        }
    }

    public void removeChunk(Chunk chunk) {
        Iterator<Map.Entry<String, Location>> iterator = beacons.entrySet().iterator();
        while (iterator.hasNext()) {
            Location location = iterator.next().getValue();
            if (location.getWorld() != null
                    && location.getWorld().equals(chunk.getWorld())
                    && (location.getBlockX() >> 4) == chunk.getX()
                    && (location.getBlockZ() >> 4) == chunk.getZ()) {
                iterator.remove();
            }
        }
    }

    public List<Location> snapshot() {
        return new ArrayList<>(beacons.values());
    }

    public void clearInvalid() {
        Iterator<Map.Entry<String, Location>> iterator = beacons.entrySet().iterator();
        while (iterator.hasNext()) {
            Location location = iterator.next().getValue();
            World world = location.getWorld();
            if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                iterator.remove();
                continue;
            }
            Block block = location.getBlock();
            if (block.getType() != Material.BEACON) {
                iterator.remove();
            }
        }
    }

    private static String key(Location location) {
        return Objects.requireNonNull(location.getWorld()).getName()
                + ";" + location.getBlockX()
                + ";" + location.getBlockY()
                + ";" + location.getBlockZ();
    }
}
