package com.frost.plots.model;

import java.util.Locale;

public enum PlotRole {
    VISITOR,
    MEMBER,
    CO_OWNER;

    public static PlotRole fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        key = switch (key) {
            case "COOWNER", "COOWN", "CO" -> "CO_OWNER";
            case "MEM", "BUILD", "BUILDER" -> "MEMBER";
            case "VISIT", "GUEST" -> "VISITOR";
            default -> key;
        };
        try {
            return PlotRole.valueOf(key);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public boolean canBuild(PlotPrivacy privacy) {
        // Visitors never build — public only means visit access, not grief rights.
        return this == MEMBER || this == CO_OWNER;
    }

    public boolean canInteract(PlotPrivacy privacy) {
        // Containers / shulkers: same as build (owner handled separately).
        return this == MEMBER || this == CO_OWNER;
    }

    public boolean canInvite() {
        return this == CO_OWNER;
    }
}
