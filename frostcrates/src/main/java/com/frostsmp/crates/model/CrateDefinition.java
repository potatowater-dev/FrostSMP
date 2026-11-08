package com.frostsmp.crates.model;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CrateDefinition {

    private final String id;
    private String displayName;
    private Material displayBlock;
    private boolean broadcastEnabled;
    private int broadcastThresholdWeight;
    private long openCooldownMs;
    private String noKeyMessage;
    private AutoKeySettings autoKey;
    private final List<LootEntry> loot;

    public CrateDefinition(String id) {
        this.id = id;
        this.displayName = id;
        this.displayBlock = Material.LIME_SHULKER_BOX;
        this.broadcastEnabled = true;
        this.broadcastThresholdWeight = 5;
        this.openCooldownMs = 3000L;
        this.noKeyMessage = null;
        this.autoKey = new AutoKeySettings();
        this.loot = new ArrayList<>();
    }

    public CrateDefinition(
            String id,
            String displayName,
            Material displayBlock,
            boolean broadcastEnabled,
            int broadcastThresholdWeight,
            long openCooldownMs,
            String noKeyMessage,
            AutoKeySettings autoKey,
            List<LootEntry> loot
    ) {
        this.id = id;
        this.displayName = displayName;
        this.displayBlock = displayBlock;
        this.broadcastEnabled = broadcastEnabled;
        this.broadcastThresholdWeight = broadcastThresholdWeight;
        this.openCooldownMs = openCooldownMs;
        this.noKeyMessage = noKeyMessage;
        this.autoKey = autoKey == null ? new AutoKeySettings() : autoKey;
        this.loot = new ArrayList<>(loot);
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

    public Material getDisplayBlock() {
        return displayBlock;
    }

    public void setDisplayBlock(Material displayBlock) {
        this.displayBlock = displayBlock;
    }

    /** @deprecated use {@link #getDisplayBlock()} */
    @Deprecated
    public Material getCandleColor() {
        return displayBlock;
    }

    /** @deprecated use {@link #setDisplayBlock(Material)} */
    @Deprecated
    public void setCandleColor(Material displayBlock) {
        this.displayBlock = displayBlock;
    }

    public boolean isBroadcastEnabled() {
        return broadcastEnabled;
    }

    public void setBroadcastEnabled(boolean broadcastEnabled) {
        this.broadcastEnabled = broadcastEnabled;
    }

    public int getBroadcastThresholdWeight() {
        return broadcastThresholdWeight;
    }

    public void setBroadcastThresholdWeight(int broadcastThresholdWeight) {
        this.broadcastThresholdWeight = broadcastThresholdWeight;
    }

    public long getOpenCooldownMs() {
        return openCooldownMs;
    }

    public void setOpenCooldownMs(long openCooldownMs) {
        this.openCooldownMs = openCooldownMs;
    }

    public String getNoKeyMessage() {
        return noKeyMessage;
    }

    public void setNoKeyMessage(String noKeyMessage) {
        this.noKeyMessage = noKeyMessage;
    }

    public AutoKeySettings getAutoKey() {
        return autoKey;
    }

    public void setAutoKey(AutoKeySettings autoKey) {
        this.autoKey = autoKey == null ? new AutoKeySettings() : autoKey;
    }

    public List<LootEntry> getLoot() {
        return Collections.unmodifiableList(loot);
    }

    public void setLoot(List<LootEntry> entries) {
        loot.clear();
        loot.addAll(entries);
    }

    public int getTotalWeight() {
        return loot.stream().mapToInt(LootEntry::getWeight).sum();
    }

    public boolean hasLoot() {
        return !loot.isEmpty() && getTotalWeight() > 0;
    }
}
