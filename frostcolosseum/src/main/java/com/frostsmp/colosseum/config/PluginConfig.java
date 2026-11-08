package com.frostsmp.colosseum.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PluginConfig {

    private final JavaPlugin plugin;

    private int intervalMinutes;
    private int lootSeconds;
    private int maxPerTeam;

    private Material wandMaterial;
    private String wandName;
    private List<String> wandLore;

    private final Map<String, String> messages = new HashMap<>();

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        intervalMinutes = Math.max(1, config.getInt("interval-minutes", 15));
        lootSeconds = Math.max(5, config.getInt("loot-seconds", 120));
        maxPerTeam = Math.max(1, config.getInt("max-per-team", 2));

        wandMaterial = Material.matchMaterial(config.getString("wand.material", "BLAZE_ROD"));
        if (wandMaterial == null) {
            wandMaterial = Material.BLAZE_ROD;
        }
        wandName = config.getString("wand.name", "<gold><bold>Colosseum Wand</bold></gold>");
        wandLore = config.getStringList("wand.lore");

        messages.clear();
        var msgSec = config.getConfigurationSection("messages");
        if (msgSec != null) {
            for (String key : msgSec.getKeys(false)) {
                messages.put(key, msgSec.getString(key, ""));
            }
        }
    }

    public String msg(String key) {
        String prefix = messages.getOrDefault("prefix", "");
        String raw = messages.get(key);
        if (raw == null) {
            return prefix + "<red>Missing message: " + key + "</red>";
        }
        return prefix + raw;
    }

    public String rawMsg(String key) {
        return messages.getOrDefault(key, "");
    }

    public int getIntervalMinutes() {
        return intervalMinutes;
    }

    public int getLootSeconds() {
        return lootSeconds;
    }

    public int getMaxPerTeam() {
        return maxPerTeam;
    }

    public Material getWandMaterial() {
        return wandMaterial;
    }

    public String getWandName() {
        return wandName;
    }

    public List<String> getWandLore() {
        return wandLore;
    }
}
