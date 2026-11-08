package com.frostsmp.wallet.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WalletStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Map<String, Long>> balances = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    public WalletStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "wallets.yml");
    }

    public void load() {
        balances.clear();
        names.clear();
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
                Map<String, Long> wallet = new ConcurrentHashMap<>();
                for (String key : section.getKeys(false)) {
                    if (key.equalsIgnoreCase("name")) {
                        String name = section.getString(key);
                        if (name != null && !name.isBlank()) {
                            names.put(uuid, name);
                        }
                        continue;
                    }
                    wallet.put(key, section.getLong(key, 0L));
                }
                balances.put(uuid, wallet);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Long>> player : balances.entrySet()) {
            String path = "players." + player.getKey();
            String name = names.get(player.getKey());
            if (name != null && !name.isBlank()) {
                config.set(path + ".name", name);
            }
            for (Map.Entry<String, Long> entry : player.getValue().entrySet()) {
                if (entry.getValue() != 0L) {
                    config.set(path + "." + entry.getKey(), entry.getValue());
                }
            }
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for wallets.yml");
            }
            File temp = new File(file.getParentFile(), "wallets.yml.tmp");
            config.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save wallets.yml: " + exception.getMessage());
        }
    }

    public List<Map.Entry<UUID, Long>> amounts(String currencyId) {
        List<Map.Entry<UUID, Long>> list = new ArrayList<>();
        for (Map.Entry<UUID, Map<String, Long>> player : balances.entrySet()) {
            long amount = player.getValue().getOrDefault(currencyId, 0L);
            if (amount > 0L) {
                list.add(Map.entry(player.getKey(), amount));
            }
        }
        return list;
    }

    public long get(UUID playerId, String currencyId) {
        Map<String, Long> wallet = balances.get(playerId);
        if (wallet == null) {
            return 0L;
        }
        return Math.max(0L, wallet.getOrDefault(currencyId, 0L));
    }

    public void set(UUID playerId, String currencyId, long amount) {
        set(playerId, currencyId, amount, null);
    }

    public void set(UUID playerId, String currencyId, long amount, String playerName) {
        if (playerName != null && !playerName.isBlank()) {
            names.put(playerId, playerName);
        }
        Map<String, Long> wallet = balances.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>());
        if (amount <= 0L) {
            wallet.remove(currencyId);
            if (wallet.isEmpty()) {
                balances.remove(playerId);
            }
        } else {
            wallet.put(currencyId, amount);
        }
        save();
    }

    public String name(UUID playerId) {
        return names.get(playerId);
    }

    public void clearAll() {
        balances.clear();
        names.clear();
        save();
    }
}
