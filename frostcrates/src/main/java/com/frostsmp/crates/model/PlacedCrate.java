package com.frostsmp.crates.model;

import org.bukkit.Location;

public final class PlacedCrate {

    private final String id;
    private final Location location;
    private final String definitionId;

    public PlacedCrate(String id, Location location, String definitionId) {
        this.id = id;
        this.location = location.clone();
        this.definitionId = definitionId;
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location.clone();
    }

    public String getDefinitionId() {
        return definitionId;
    }
}
