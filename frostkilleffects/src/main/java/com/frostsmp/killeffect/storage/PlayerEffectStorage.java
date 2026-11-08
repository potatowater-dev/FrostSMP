package com.frostsmp.killeffect.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerEffectStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Set<String>> claimed = new ConcurrentHashMap<>();
    private final Map<UUID, String> selected = new ConcurrentHashMap<>();

    public PlayerEffectStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    public void load() {
        claimed.clear();
        selected.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String id : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(id);
                ConfigurationSection section = players.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }
                Set<String> owned = ConcurrentHashMap.newKeySet();
                owned.addAll(section.getStringList("claimed"));
                claimed.put(uuid, owned);
                String sel = section.getString("selected", "");
                if (sel != null && !sel.isBlank()) {
                    selected.put(uuid, sel.toLowerCase());
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        Set<UUID> ids = new HashSet<>();
        ids.addAll(claimed.keySet());
        ids.addAll(selected.keySet());
        for (UUID uuid : ids) {
            String path = "players." + uuid;
            Set<String> owned = claimed.getOrDefault(uuid, Collections.emptySet());
            if (!owned.isEmpty()) {
                config.set(path + ".claimed", owned.stream().sorted().toList());
            }
            String sel = selected.get(uuid);
            if (sel != null && !sel.isBlank()) {
                config.set(path + ".selected", sel);
            }
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for players.yml");
            }
            File temp = new File(file.getParentFile(), "players.yml.tmp");
            config.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save players.yml: " + exception.getMessage());
        }
    }

    public boolean hasClaimed(UUID playerId, String effectId) {
        Set<String> owned = claimed.get(playerId);
        return owned != null && owned.contains(effectId.toLowerCase());
    }

    public Set<String> claimed(UUID playerId) {
        Set<String> owned = claimed.get(playerId);
        if (owned == null) {
            return Set.of();
        }
        return Set.copyOf(owned);
    }

    public boolean claim(UUID playerId, String effectId) {
        String key = effectId.toLowerCase();
        Set<String> owned = claimed.computeIfAbsent(playerId, id -> ConcurrentHashMap.newKeySet());
        if (!owned.add(key)) {
            return false;
        }
        if (!selected.containsKey(playerId)) {
            selected.put(playerId, key);
        }
        save();
        return true;
    }

    public String selected(UUID playerId) {
        return selected.get(playerId);
    }

    public void select(UUID playerId, String effectId) {
        if (effectId == null || effectId.isBlank()) {
            selected.remove(playerId);
        } else {
            selected.put(playerId, effectId.toLowerCase());
        }
        save();
    }
}
