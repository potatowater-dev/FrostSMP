package com.frostsmp.crates.util;

import org.bukkit.Location;

public final class LocationKey {

    private LocationKey() {
    }

    public static String from(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}
