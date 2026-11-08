package com.frostsmp.crates.model;

public final class AutoKeySettings {

    private boolean enabled;
    private String group;
    private String permission;
    private long intervalMs;
    private int amount;

    public AutoKeySettings() {
        this.enabled = false;
        this.group = "";
        this.permission = "";
        this.intervalMs = 86_400_000L;
        this.amount = 1;
    }

    public AutoKeySettings(boolean enabled, String group, String permission, long intervalMs, int amount) {
        this.enabled = enabled;
        this.group = group == null ? "" : group.trim();
        this.permission = permission == null ? "" : permission.trim();
        this.intervalMs = Math.max(60_000L, intervalMs);
        this.amount = Math.max(1, amount);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group == null ? "" : group.trim();
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission == null ? "" : permission.trim();
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = Math.max(60_000L, intervalMs);
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(1, amount);
    }

    public boolean usesGroup() {
        return group != null && !group.isBlank();
    }

    public boolean isConfigured() {
        return enabled && (usesGroup() || (permission != null && !permission.isBlank()));
    }
}
