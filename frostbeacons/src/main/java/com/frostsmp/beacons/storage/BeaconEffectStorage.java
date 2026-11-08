package com.frostsmp.beacons.storage;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class BeaconEffectStorage {

    public static final class Selection {
        private final PotionEffectType primary;
        private final PotionEffectType secondary;

        public Selection(PotionEffectType primary, PotionEffectType secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        public PotionEffectType primary() {
            return primary;
        }

        public PotionEffectType secondary() {
            return secondary;
        }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Selection> selections = new HashMap<>();

    public BeaconEffectStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "beacon-effects.yml");
    }

    public void load() {
        selections.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("beacons");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String primaryName = section.getString(key + ".primary");
            String secondaryName = section.getString(key + ".secondary");
            selections.put(key, new Selection(parse(primaryName), parse(secondaryName)));
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Selection> entry : selections.entrySet()) {
            String path = "beacons." + entry.getKey();
            config.set(path + ".primary", entry.getValue().primary() == null
                    ? null
                    : entry.getValue().primary().getKey().getKey());
            config.set(path + ".secondary", entry.getValue().secondary() == null
                    ? null
                    : entry.getValue().secondary().getKey().getKey());
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for beacon-effects.yml");
            }
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save beacon-effects.yml: " + exception.getMessage());
        }
    }

    public void put(Location location, PotionEffectType primary, PotionEffectType secondary) {
        selections.put(BeamLinkStorage.key(location), new Selection(primary, secondary));
        save();
    }

    public Selection get(Location location) {
        return selections.get(BeamLinkStorage.key(location));
    }

    public void remove(Location location) {
        if (selections.remove(BeamLinkStorage.key(location)) != null) {
            save();
        }
    }

    private static PotionEffectType parse(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.contains(":")) {
            key = key.substring(key.indexOf(':') + 1);
        }
        return Registry.EFFECT.get(NamespacedKey.minecraft(key));
    }
}
