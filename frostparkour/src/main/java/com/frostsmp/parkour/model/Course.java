package com.frostsmp.parkour.model;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Course {

    private final String id;
    private String displayName;
    private Location startLocation;
    private Location endLocation;
    private final List<Location> checkpoints = new ArrayList<>();
    private Double customFallY;

    public Course(String id, String displayName) {
        this.id = id.toLowerCase();
        this.displayName = displayName != null && !displayName.isBlank() ? displayName : id;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(Location startLocation) {
        this.startLocation = startLocation != null ? startLocation.clone() : null;
    }

    public Location getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(Location endLocation) {
        this.endLocation = endLocation != null ? endLocation.clone() : null;
    }

    public List<Location> getCheckpoints() {
        return Collections.unmodifiableList(checkpoints);
    }

    public void addCheckpoint(Location location) {
        if (location != null) {
            checkpoints.add(location.clone());
        }
    }

    public boolean removeCheckpoint(int index) {
        if (index >= 0 && index < checkpoints.size()) {
            checkpoints.remove(index);
            return true;
        }
        return false;
    }

    public void clearCheckpoints() {
        checkpoints.clear();
    }

    public void setCheckpoints(List<Location> list) {
        checkpoints.clear();
        if (list != null) {
            for (Location loc : list) {
                if (loc != null) {
                    checkpoints.add(loc.clone());
                }
            }
        }
    }

    public Double getCustomFallY() {
        return customFallY;
    }

    public void setCustomFallY(Double customFallY) {
        this.customFallY = customFallY;
    }

    public boolean isConfigured() {
        return startLocation != null && endLocation != null;
    }
}
