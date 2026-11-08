package com.frostsmp.bounty.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Bounty {

    public record Contribution(UUID placerId, String placerName, List<ItemStack> items) {
        public Contribution {
            items = cloneItems(items);
            placerName = placerName == null || placerName.isBlank() ? "Unknown" : placerName;
        }

        public List<ItemStack> items() {
            return cloneItems(items);
        }
    }

    private final UUID id;
    private final UUID targetId;
    private String targetName;
    private final long createdAt;
    private final List<Contribution> contributions = new ArrayList<>();

    public Bounty(
            UUID id,
            UUID targetId,
            String targetName,
            UUID placerId,
            String placerName,
            List<ItemStack> rewards,
            long createdAt
    ) {
        this.id = id;
        this.targetId = targetId;
        this.targetName = targetName == null ? "Unknown" : targetName;
        this.createdAt = createdAt;
        addContribution(placerId, placerName, rewards);
    }

    public Bounty(
            UUID id,
            UUID targetId,
            String targetName,
            long createdAt,
            List<Contribution> contributions
    ) {
        this.id = id;
        this.targetId = targetId;
        this.targetName = targetName == null ? "Unknown" : targetName;
        this.createdAt = createdAt;
        if (contributions != null) {
            for (Contribution contribution : contributions) {
                if (contribution != null && !contribution.items().isEmpty()) {
                    this.contributions.add(contribution);
                }
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        if (targetName != null && !targetName.isBlank()) {
            this.targetName = targetName;
        }
    }

    /** First placer (legacy helper). */
    public UUID getPlacerId() {
        return contributions.isEmpty() ? null : contributions.get(0).placerId();
    }

    public String getPlacerName() {
        Set<String> names = new LinkedHashSet<>();
        for (Contribution contribution : contributions) {
            names.add(contribution.placerName());
        }
        if (names.isEmpty()) {
            return "Unknown";
        }
        return String.join(", ", names);
    }

    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        for (Contribution contribution : contributions) {
            rewards.addAll(contribution.items());
        }
        return rewards;
    }

    public int rewardSlotCount() {
        int count = 0;
        for (Contribution contribution : contributions) {
            count += contribution.items().size();
        }
        return count;
    }

    public boolean canFit(int additionalSlots, int maxSlots) {
        return rewardSlotCount() + additionalSlots <= maxSlots;
    }

    public void addContribution(UUID placerId, String placerName, List<ItemStack> rewards) {
        List<ItemStack> items = cloneItems(rewards);
        if (items.isEmpty() || placerId == null) {
            return;
        }
        contributions.add(new Contribution(placerId, placerName, items));
    }

    public List<Contribution> getContributions() {
        return Collections.unmodifiableList(new ArrayList<>(contributions));
    }

    /** Rewards grouped by placer for refunds. */
    public Map<UUID, List<ItemStack>> rewardsByPlacer() {
        Map<UUID, List<ItemStack>> byPlacer = new LinkedHashMap<>();
        for (Contribution contribution : contributions) {
            byPlacer.computeIfAbsent(contribution.placerId(), id -> new ArrayList<>())
                    .addAll(contribution.items());
        }
        return byPlacer;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    private static List<ItemStack> cloneItems(List<ItemStack> items) {
        List<ItemStack> copy = new ArrayList<>();
        if (items == null) {
            return copy;
        }
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                copy.add(item.clone());
            }
        }
        return copy;
    }
}
