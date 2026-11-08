package com.frostsmp.bounty.storage;

import com.frostsmp.bounty.FrostBountyPlugin;
import com.frostsmp.bounty.model.Bounty;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BountyStorage {

    private final FrostBountyPlugin plugin;
    private final File file;
    private final List<Bounty> bounties = new ArrayList<>();

    public BountyStorage(FrostBountyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bounties.yml");
    }

    public void load() {
        bounties.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (Map<?, ?> map : yaml.getMapList("bounties")) {
            Bounty bounty = readBounty(map);
            if (bounty != null) {
                bounties.add(bounty);
            }
        }
    }

    private Bounty readBounty(Map<?, ?> map) {
        try {
            UUID id = UUID.fromString(String.valueOf(map.get("id")));
            UUID targetId = UUID.fromString(String.valueOf(map.get("target-id")));
            Object targetNameObj = map.get("target-name");
            String targetName = targetNameObj == null ? "Unknown" : String.valueOf(targetNameObj);
            long createdAt = map.get("created-at") instanceof Number number
                    ? number.longValue()
                    : System.currentTimeMillis();

            List<Bounty.Contribution> contributions = new ArrayList<>();
            Object contributionsRaw = map.get("contributions");
            if (contributionsRaw instanceof List<?> contributionMaps) {
                for (Object entry : contributionMaps) {
                    if (!(entry instanceof Map<?, ?> contributionMap)) {
                        continue;
                    }
                    UUID placerId = UUID.fromString(String.valueOf(contributionMap.get("placer-id")));
                    Object placerNameObj = contributionMap.get("placer-name");
                    String placerName = placerNameObj == null ? "Unknown" : String.valueOf(placerNameObj);
                    List<ItemStack> rewards = readRewards(contributionMap.get("rewards"));
                    if (!rewards.isEmpty()) {
                        contributions.add(new Bounty.Contribution(placerId, placerName, rewards));
                    }
                }
            }

            // Legacy flat format
            if (contributions.isEmpty()) {
                UUID placerId = UUID.fromString(String.valueOf(map.get("placer-id")));
                Object placerNameObj = map.get("placer-name");
                String placerName = placerNameObj == null ? "Unknown" : String.valueOf(placerNameObj);
                List<ItemStack> rewards = readRewards(map.get("rewards"));
                if (rewards.isEmpty()) {
                    return null;
                }
                contributions.add(new Bounty.Contribution(placerId, placerName, rewards));
            }

            return new Bounty(id, targetId, targetName, createdAt, contributions);
        } catch (Exception exception) {
            plugin.getLogger().warning("Skipping invalid bounty entry: " + exception.getMessage());
            return null;
        }
    }

    private static List<ItemStack> readRewards(Object rewardsRaw) {
        List<ItemStack> rewards = new ArrayList<>();
        if (rewardsRaw instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof ItemStack stack) {
                    rewards.add(stack.clone());
                }
            }
        }
        return rewards;
    }

    public void save() {
        FileConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Bounty bounty : bounties) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", bounty.getId().toString());
            map.put("target-id", bounty.getTargetId().toString());
            map.put("target-name", bounty.getTargetName());
            map.put("created-at", bounty.getCreatedAt());

            List<Map<String, Object>> contributionMaps = new ArrayList<>();
            for (Bounty.Contribution contribution : bounty.getContributions()) {
                Map<String, Object> contributionMap = new LinkedHashMap<>();
                contributionMap.put("placer-id", contribution.placerId().toString());
                contributionMap.put("placer-name", contribution.placerName());
                contributionMap.put("rewards", contribution.items());
                contributionMaps.add(contributionMap);
            }
            map.put("contributions", contributionMaps);

            // Keep flat fields for older tooling / readability
            map.put("placer-id", bounty.getPlacerId() == null ? "" : bounty.getPlacerId().toString());
            map.put("placer-name", bounty.getPlacerName());
            map.put("rewards", bounty.getRewards());
            list.add(map);
        }
        yaml.set("bounties", list);
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save bounties.yml: " + exception.getMessage());
        }
    }

    public List<Bounty> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(bounties));
    }

    public void add(Bounty bounty) {
        bounties.add(bounty);
        save();
    }

    public void touch() {
        save();
    }

    /**
     * First bounty on this target that can fit {@code additionalSlots} reward stacks.
     */
    public Bounty findMergeable(UUID targetId, int additionalSlots, int maxSlots) {
        for (Bounty bounty : bounties) {
            if (bounty.getTargetId().equals(targetId) && bounty.canFit(additionalSlots, maxSlots)) {
                return bounty;
            }
        }
        return null;
    }

    public List<Bounty> removeForTarget(UUID targetId) {
        List<Bounty> removed = new ArrayList<>();
        bounties.removeIf(bounty -> {
            if (bounty.getTargetId().equals(targetId)) {
                removed.add(bounty);
                return true;
            }
            return false;
        });
        if (!removed.isEmpty()) {
            save();
        }
        return removed;
    }

    public boolean isEmpty() {
        return bounties.isEmpty();
    }
}
