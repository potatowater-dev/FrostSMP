package com.frostsmp.colosseum.service;

import com.frostsmp.colosseum.FrostColosseumPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;

public final class RewardService {

    private final FrostColosseumPlugin plugin;
    private final File file;
    private FileConfiguration yaml;
    private ItemStack reward;

    public RewardService(FrostColosseumPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "reward.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe("Could not create reward.yml: " + exception.getMessage());
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        reward = yaml.getItemStack("reward");
    }

    public void save() {
        if (yaml == null) {
            yaml = new YamlConfiguration();
        }
        yaml.set("reward", reward);
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save reward.yml: " + exception.getMessage());
        }
    }

    public ItemStack getReward() {
        return reward == null ? null : reward.clone();
    }

    public boolean hasReward() {
        return reward != null && !reward.getType().isAir();
    }

    public void setReward(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            this.reward = null;
        } else {
            this.reward = item.clone();
        }
        save();
    }

    public void clearReward() {
        this.reward = null;
        save();
    }
}
