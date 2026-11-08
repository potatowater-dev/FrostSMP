package com.frostsmp.beacons.service;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Draws a visible beacon-like beam from the beacon up to the end Y.
 * Stained glass / panes mix the beam color like a vanilla beacon.
 * (Vanilla beams will not render underground — the client clears them without sky access.)
 */
public final class BeaconBeamService {

    public boolean applyBeam(Location beaconLocation, int endY, int tier) {
        World world = beaconLocation.getWorld();
        if (world == null) {
            return false;
        }

        double x = beaconLocation.getBlockX() + 0.5;
        double z = beaconLocation.getBlockZ() + 0.5;
        int startY = beaconLocation.getBlockY() + 1;
        int stopY = Math.min(endY, world.getMaxHeight() - 1);
        if (stopY < startY) {
            return false;
        }

        float[] rgb = new float[]{1.0f, 1.0f, 1.0f};

        for (int y = startY; y <= stopY; y++) {
            Block block = world.getBlockAt(beaconLocation.getBlockX(), y, beaconLocation.getBlockZ());
            DyeColor dye = dyeFromGlass(block.getType());
            if (dye != null) {
                float[] glass = diffuse(dye);
                rgb[0] = (rgb[0] + glass[0]) / 2.0f;
                rgb[1] = (rgb[1] + glass[1]) / 2.0f;
                rgb[2] = (rgb[2] + glass[2]) / 2.0f;
            }

            Color color = Color.fromRGB(toByte(rgb[0]), toByte(rgb[1]), toByte(rgb[2]));
            Particle.DustOptions dust = new Particle.DustOptions(color, 1.35f);
            double py = y + 0.5;

            // Colored outer beam
            world.spawnParticle(Particle.DUST, x, py, z, 3, 0.08, 0.2, 0.08, 0, dust);
            // Bright core (reads more like a real beacon shaft)
            world.spawnParticle(Particle.END_ROD, x, py, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        return true;
    }

    public void clearBeam(Location beaconLocation) {
        // Particles are transient — nothing to clear.
    }

    private static DyeColor dyeFromGlass(Material material) {
        String name = material.name();
        if (!name.contains("STAINED_GLASS")) {
            return null;
        }
        for (DyeColor color : DyeColor.values()) {
            if (name.startsWith(color.name() + "_")) {
                return color;
            }
        }
        return null;
    }

    private static float[] diffuse(DyeColor color) {
        Color bukkit = color.getColor();
        return new float[]{
                bukkit.getRed() / 255.0f,
                bukkit.getGreen() / 255.0f,
                bukkit.getBlue() / 255.0f
        };
    }

    private static int toByte(float value) {
        return Math.min(255, Math.max(0, (int) (value * 255.0f)));
    }
}
