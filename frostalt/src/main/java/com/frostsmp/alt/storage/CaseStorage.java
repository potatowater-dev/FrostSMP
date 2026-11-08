package com.frostsmp.alt.storage;

import com.frostsmp.alt.model.AltCase;
import com.frostsmp.alt.model.CaseStatus;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CaseStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<Integer, AltCase> cases = new HashMap<>();
    private int nextCaseId = 1;

    public CaseStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cases.yml");
    }

    public void load() {
        cases.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        nextCaseId = config.getInt("next-case-id", 1);
        ConfigurationSection section = config.getConfigurationSection("cases");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                AltCase altCase = readCase(id, section.getConfigurationSection(key));
                if (altCase != null) {
                    cases.put(id, altCase);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        config.set("next-case-id", nextCaseId);
        for (AltCase altCase : cases.values()) {
            String path = "cases." + altCase.getId();
            config.set(path + ".ip", altCase.getIp());
            config.set(path + ".flagged-player", altCase.getFlaggedPlayerId().toString());
            config.set(path + ".flagged-name", altCase.getFlaggedPlayerName());
            config.set(path + ".linked-accounts", altCase.getLinkedAccountIds().stream().map(UUID::toString).toList());
            config.set(path + ".playtime-minutes", altCase.getPlaytimeMinutes());
            config.set(path + ".created-at", altCase.getCreatedAt());
            config.set(path + ".status", altCase.getStatus().name());
            config.set(path + ".appeal-text", altCase.getAppealText());
            if (altCase.getAppealSubmittedAt() != null) {
                config.set(path + ".appeal-submitted-at", altCase.getAppealSubmittedAt());
            }
            if (altCase.getSavedInventory() != null) {
                config.set(path + ".inventory", altCase.getSavedInventory());
            }
            if (altCase.getSavedArmor() != null) {
                config.set(path + ".armor", altCase.getSavedArmor());
            }
            if (altCase.getSavedOffhand() != null) {
                config.set(path + ".offhand", altCase.getSavedOffhand());
            }
        }
        try {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                plugin.getLogger().warning("Could not create FrostAlt data folder.");
            }
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save cases.yml: " + exception.getMessage());
        }
    }

    public AltCase createCase(String ip, UUID flaggedPlayerId, String flaggedPlayerName) {
        AltCase altCase = new AltCase(nextCaseId++, ip, flaggedPlayerId, flaggedPlayerName, System.currentTimeMillis());
        cases.put(altCase.getId(), altCase);
        save();
        return altCase;
    }

    public AltCase getCase(int id) {
        return cases.get(id);
    }

    public List<AltCase> getOpenCases() {
        List<AltCase> open = new ArrayList<>();
        for (AltCase altCase : cases.values()) {
            if (altCase.isOpen()) {
                open.add(altCase);
            }
        }
        open.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return open;
    }

    public AltCase findOpenCaseForPlayer(UUID playerId) {
        for (AltCase altCase : cases.values()) {
            if (altCase.isOpen() && altCase.getFlaggedPlayerId().equals(playerId)) {
                return altCase;
            }
        }
        return null;
    }

    private AltCase readCase(int id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        try {
            UUID flagged = UUID.fromString(section.getString("flagged-player"));
            AltCase altCase = new AltCase(
                    id,
                    section.getString("ip", "unknown"),
                    flagged,
                    section.getString("flagged-name", "unknown"),
                    section.getLong("created-at", 0L)
            );
            for (String linked : section.getStringList("linked-accounts")) {
                altCase.getLinkedAccountIds().add(UUID.fromString(linked));
            }
            ConfigurationSection playtime = section.getConfigurationSection("playtime-minutes");
            if (playtime != null) {
                for (String name : playtime.getKeys(false)) {
                    altCase.getPlaytimeMinutes().put(name, playtime.getInt(name));
                }
            }
            altCase.setStatus(CaseStatus.valueOf(section.getString("status", "OPEN")));
            altCase.setAppealText(section.getString("appeal-text"));
            if (section.contains("appeal-submitted-at")) {
                altCase.setAppealSubmittedAt(section.getLong("appeal-submitted-at"));
            }
            if (section.contains("inventory")) {
                List<?> inventory = section.getList("inventory");
                if (inventory != null) {
                    altCase.setSavedInventory(inventory.toArray(new ItemStack[0]));
                }
            }
            if (section.contains("armor")) {
                List<?> armor = section.getList("armor");
                if (armor != null) {
                    altCase.setSavedArmor(armor.toArray(new ItemStack[0]));
                }
            }
            if (section.contains("offhand")) {
                altCase.setSavedOffhand(section.getItemStack("offhand"));
            }
            return altCase;
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to load case " + id + ": " + exception.getMessage());
            return null;
        }
    }
}
