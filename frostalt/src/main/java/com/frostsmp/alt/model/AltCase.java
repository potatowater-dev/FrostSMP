package com.frostsmp.alt.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AltCase {

    private final int id;
    private final String ip;
    private final UUID flaggedPlayerId;
    private String flaggedPlayerName;
    private final List<UUID> linkedAccountIds = new ArrayList<>();
    private final Map<String, Integer> playtimeMinutes = new HashMap<>();
    private final long createdAt;
    private CaseStatus status;
    private String appealText;
    private Long appealSubmittedAt;
    private ItemStack[] savedInventory;
    private ItemStack[] savedArmor;
    private ItemStack savedOffhand;

    public AltCase(
            int id,
            String ip,
            UUID flaggedPlayerId,
            String flaggedPlayerName,
            long createdAt
    ) {
        this.id = id;
        this.ip = ip;
        this.flaggedPlayerId = flaggedPlayerId;
        this.flaggedPlayerName = flaggedPlayerName;
        this.createdAt = createdAt;
        this.status = CaseStatus.OPEN;
    }

    public int getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public UUID getFlaggedPlayerId() {
        return flaggedPlayerId;
    }

    public String getFlaggedPlayerName() {
        return flaggedPlayerName;
    }

    public void setFlaggedPlayerName(String flaggedPlayerName) {
        this.flaggedPlayerName = flaggedPlayerName;
    }

    public List<UUID> getLinkedAccountIds() {
        return linkedAccountIds;
    }

    public Map<String, Integer> getPlaytimeMinutes() {
        return playtimeMinutes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public String getAppealText() {
        return appealText;
    }

    public void setAppealText(String appealText) {
        this.appealText = appealText;
    }

    public Long getAppealSubmittedAt() {
        return appealSubmittedAt;
    }

    public void setAppealSubmittedAt(Long appealSubmittedAt) {
        this.appealSubmittedAt = appealSubmittedAt;
    }

    public ItemStack[] getSavedInventory() {
        return savedInventory;
    }

    public void setSavedInventory(ItemStack[] savedInventory) {
        this.savedInventory = savedInventory;
    }

    public ItemStack[] getSavedArmor() {
        return savedArmor;
    }

    public void setSavedArmor(ItemStack[] savedArmor) {
        this.savedArmor = savedArmor;
    }

    public ItemStack getSavedOffhand() {
        return savedOffhand;
    }

    public void setSavedOffhand(ItemStack savedOffhand) {
        this.savedOffhand = savedOffhand;
    }

    public boolean isOpen() {
        return status == CaseStatus.OPEN;
    }
}
