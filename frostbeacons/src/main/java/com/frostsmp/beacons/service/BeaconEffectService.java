package com.frostsmp.beacons.service;

import com.frostsmp.beacons.config.PluginConfig;
import com.frostsmp.beacons.storage.BeaconEffectStorage;
import com.frostsmp.beacons.storage.BeamLinkStorage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Beacon;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;

public final class BeaconEffectService {

    private final PluginConfig config;
    private final PyramidService pyramidService;
    private final BeaconRegistry registry;
    private final BeamLinkStorage beamLinks;
    private final BeaconEffectStorage effectStorage;
    private final BeaconBeamService beamService;

    public BeaconEffectService(
            PluginConfig config,
            PyramidService pyramidService,
            BeaconRegistry registry,
            BeamLinkStorage beamLinks,
            BeaconEffectStorage effectStorage,
            BeaconBeamService beamService
    ) {
        this.config = config;
        this.pyramidService = pyramidService;
        this.registry = registry;
        this.beamLinks = beamLinks;
        this.effectStorage = effectStorage;
        this.beamService = beamService;
    }

    public void tick() {
        registry.clearInvalid();
        for (Location location : registry.snapshot()) {
            tickBeacon(location);
        }
        // Also tick beacons that have a saved beam end even if they weren't scanned yet
        for (var entry : beamLinks.snapshot().entrySet()) {
            Location beacon = parseKey(entry.getKey());
            if (beacon != null && beacon.getBlock().getType() == Material.BEACON) {
                registry.register(beacon);
            }
        }
    }

    private void tickBeacon(Location location) {
        if (location.getWorld() == null) {
            return;
        }
        if (location.getBlock().getType() != Material.BEACON) {
            registry.unregister(location);
            return;
        }

        Location end = beamLinks.getEnd(location);
        if (end == null || end.getWorld() == null || !end.getWorld().equals(location.getWorld())) {
            return;
        }
        if (end.getBlockY() <= location.getBlockY()) {
            return;
        }
        if (end.getBlockX() != location.getBlockX() || end.getBlockZ() != location.getBlockZ()) {
            return;
        }

        // Always show the beam when start/end are set
        beamService.applyBeam(location, end.getBlockY(), 1);

        BlockState state = location.getBlock().getState();
        if (!(state instanceof Beacon beacon)) {
            return;
        }

        int tier = Math.max(beacon.getTier(), pyramidService.calculateTier(location));
        if (tier <= 0) {
            return;
        }

        rememberEffects(location, beacon);

        PotionEffectType primary = null;
        PotionEffectType secondary = null;
        if (beacon.getPrimaryEffect() != null) {
            primary = beacon.getPrimaryEffect().getType();
        }
        if (beacon.getSecondaryEffect() != null) {
            secondary = beacon.getSecondaryEffect().getType();
        }
        if (primary == null && secondary == null) {
            BeaconEffectStorage.Selection saved = effectStorage.get(location);
            if (saved != null) {
                primary = saved.primary();
                secondary = saved.secondary();
            }
        }
        if (primary == null && secondary == null) {
            return;
        }

        double range = 10.0 + (tier * 10.0);
        Collection<Player> players = location.getWorld().getPlayers();
        int duration = config.getEffectDurationTicks();
        for (Player player : players) {
            if (!player.getWorld().equals(location.getWorld())) {
                continue;
            }
            if (player.getLocation().distanceSquared(location) > range * range) {
                continue;
            }
            if (primary != null) {
                int amplifier = tier >= 4 && (secondary == null || secondary.equals(primary)) ? 1 : 0;
                player.addPotionEffect(new PotionEffect(primary, duration, amplifier, true, true));
            }
            if (secondary != null && primary != null && !secondary.equals(primary) && tier >= 4) {
                player.addPotionEffect(new PotionEffect(secondary, duration, 0, true, true));
            }
        }
    }

    private void rememberEffects(Location location, Beacon beacon) {
        PotionEffectType primary = beacon.getPrimaryEffect() != null ? beacon.getPrimaryEffect().getType() : null;
        PotionEffectType secondary = beacon.getSecondaryEffect() != null ? beacon.getSecondaryEffect().getType() : null;
        if (primary != null || secondary != null) {
            effectStorage.put(location, primary, secondary);
        }
    }

    private static Location parseKey(String key) {
        String[] parts = key.split(";");
        if (parts.length != 4) {
            return null;
        }
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        try {
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
