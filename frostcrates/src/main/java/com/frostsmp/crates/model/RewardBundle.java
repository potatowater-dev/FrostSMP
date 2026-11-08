package com.frostsmp.crates.model;

import com.frostsmp.crates.service.PermissionLookupService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class RewardBundle {

    private final String id;
    private final String displayName;
    private final String group;
    private final String permission;
    private final long intervalMs;
    private final List<KeyGrant> keys;

    public RewardBundle(
            String id,
            String displayName,
            String group,
            String permission,
            long intervalMs,
            List<KeyGrant> keys
    ) {
        this.id = id == null ? "" : id.trim().toLowerCase();
        this.displayName = displayName == null || displayName.isBlank() ? this.id : displayName.trim();
        this.group = group == null ? "" : group.trim();
        this.permission = permission == null ? "" : permission.trim();
        this.intervalMs = intervalMs;
        this.keys = keys == null ? List.of() : List.copyOf(keys);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGroup() {
        return group;
    }

    public String getPermission() {
        return permission;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public List<KeyGrant> getKeys() {
        return keys;
    }

    public boolean isConfigured() {
        return !id.isBlank() && !keys.isEmpty() && intervalMs > 0L
                && (!group.isBlank() || !permission.isBlank());
    }

    public boolean matches(PermissionLookupService permissionLookup, UUID playerId) {
        if (!group.isBlank()) {
            return permissionLookup.isInGroup(playerId, group);
        }
        if (!permission.isBlank()) {
            return permissionLookup.hasPermission(playerId, permission);
        }
        return false;
    }

    public List<KeyGrant> scaledKeys(int periods) {
        if (periods <= 1) {
            return keys;
        }
        List<KeyGrant> scaled = new ArrayList<>(keys.size());
        for (KeyGrant grant : keys) {
            scaled.add(new KeyGrant(grant.crateId(), grant.amount() * periods));
        }
        return scaled;
    }

    public String formatGrantSummary() {
        if (keys.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (KeyGrant grant : keys) {
            parts.add(grant.amount() + " " + grant.crateId());
        }
        return String.join(", ", parts);
    }

    public static List<RewardBundle> unmodifiableList(List<RewardBundle> bundles) {
        return bundles == null ? List.of() : Collections.unmodifiableList(bundles);
    }
}
