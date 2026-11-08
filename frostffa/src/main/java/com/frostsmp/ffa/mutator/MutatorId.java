package com.frostsmp.ffa.mutator;

import java.util.Locale;

public enum MutatorId {
    SHRINKING_BORDER("shrinking_border", "Shrinking Border"),
    FOG_OF_WAR("fog_of_war", "Fog of War"),
    UHC("uhc", "UHC"),
    STRENGTH_III("strength_iii", "Strength III"),
    RANDOM_EFFECTS("random_effects", "Random Effects"),
    BLACKOUT("blackout", "Blackout"),
    MACE_ROULETTE("mace_roulette", "Mace Roulette");

    private final String id;
    private final String displayName;

    MutatorId(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static MutatorId fromId(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (key.equals("strength") || key.equals("strength3") || key.equals("strength_3") || key.equals("str3")) {
            return STRENGTH_III;
        }
        if (key.equals("random") || key.equals("randomeffects") || key.equals("random_effect")) {
            return RANDOM_EFFECTS;
        }
        if (key.equals("border") || key.equals("shrink")) {
            return SHRINKING_BORDER;
        }
        if (key.equals("fog")) {
            return FOG_OF_WAR;
        }
        if (key.equals("mace") || key.equals("roulette") || key.equals("maceroulette")) {
            return MACE_ROULETTE;
        }
        for (MutatorId value : values()) {
            if (value.id.equals(key) || value.name().equalsIgnoreCase(key)) {
                return value;
            }
        }
        return null;
    }
}
