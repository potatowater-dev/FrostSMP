package com.frostsmp.crates.config;

import com.frostsmp.crates.FrostCratesPlugin;
import com.frostsmp.crates.model.AutoKeySettings;
import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.model.LootEntry;
import com.frostsmp.crates.util.BlockMaterialUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class CrateDefinitionStorage {

    private final FrostCratesPlugin plugin;
    private final File definitionsFolder;
    private final Map<String, CrateDefinition> definitions = new HashMap<>();

    public CrateDefinitionStorage(FrostCratesPlugin plugin) {
        this.plugin = plugin;
        this.definitionsFolder = new File(plugin.getDataFolder(), "definitions");
    }

    public void load() {
        if (!definitionsFolder.exists() && !definitionsFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create definitions folder.");
        }
        extractBundledDefaults();
        definitions.clear();
        File[] files = definitionsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            plugin.getLogger().severe("Failed to read crate definitions folder: " + definitionsFolder.getAbsolutePath());
            return;
        }
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4);
            definitions.put(id.toLowerCase(), loadFile(id.toLowerCase(), file));
        }
        if (definitions.isEmpty()) {
            plugin.getLogger().warning("No crate definitions loaded from " + definitionsFolder.getAbsolutePath());
        } else {
            plugin.getLogger().info("Loaded " + definitions.size() + " crate definition(s).");
        }
    }

    private void extractBundledDefaults() {
        String[] defaults = {"resource", "booster", "kit"};
        for (String id : defaults) {
            File target = new File(definitionsFolder, id + ".yml");
            if (target.exists()) {
                continue;
            }
            String resourcePath = "definitions/" + id + ".yml";
            if (plugin.getResource(resourcePath) == null) {
                continue;
            }
            plugin.saveResource(resourcePath, false);
            plugin.getLogger().info("Created default crate definition: " + id);
        }
    }

    public CrateDefinition get(String id) {
        return definitions.get(id.toLowerCase());
    }

    public Collection<CrateDefinition> getAll() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public List<String> getIds() {
        return definitions.keySet().stream().sorted().toList();
    }

    public CrateDefinition getOrCreate(String id) {
        String key = id.toLowerCase();
        CrateDefinition existing = definitions.get(key);
        if (existing != null) {
            return existing;
        }
        CrateDefinition created = new CrateDefinition(key);
        definitions.put(key, created);
        save(created);
        return created;
    }

    public boolean delete(String id) {
        String key = id.toLowerCase();
        if (!definitions.containsKey(key)) {
            return false;
        }
        definitions.remove(key);
        File file = new File(definitionsFolder, key + ".yml");
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete definition file: " + file.getName());
        }
        return true;
    }

    public void save(CrateDefinition definition) {
        File file = new File(definitionsFolder, definition.getId() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("display-name", definition.getDisplayName());
        yaml.set("display-block", definition.getDisplayBlock().name());
        yaml.set("candle-color", definition.getDisplayBlock().name());
        yaml.set("broadcast-enabled", definition.isBroadcastEnabled());
        yaml.set("broadcast-threshold-weight", definition.getBroadcastThresholdWeight());
        yaml.set("open-cooldown-ms", definition.getOpenCooldownMs());
        AutoKeySettings autoKey = definition.getAutoKey();
        yaml.set("auto-key.enabled", autoKey.isEnabled());
        yaml.set("auto-key.group", autoKey.getGroup());
        yaml.set("auto-key.permission", autoKey.getPermission());
        yaml.set("auto-key.interval-ms", autoKey.getIntervalMs());
        yaml.set("auto-key.amount", autoKey.getAmount());
        if (definition.getNoKeyMessage() != null) {
            yaml.set("no-key-message", definition.getNoKeyMessage());
        }
        List<Map<String, Object>> lootList = new ArrayList<>();
        for (LootEntry entry : definition.getLoot()) {
            Map<String, Object> map = new HashMap<>();
            map.put("item", entry.getItem());
            map.put("weight", entry.getWeight());
            lootList.add(map);
        }
        yaml.set("loot", lootList);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save crate definition " + definition.getId(), e);
        }
    }

    private CrateDefinition loadFile(String id, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String displayName = yaml.getString("display-name", id);
        Material displayBlock = resolveDisplayBlock(yaml);
        boolean broadcast = yaml.getBoolean("broadcast-enabled", true);
        int threshold = yaml.getInt("broadcast-threshold-weight", 5);
        long cooldown = yaml.getLong("open-cooldown-ms", 3000L);
        AutoKeySettings autoKey = loadAutoKey(yaml);
        String noKey = yaml.getString("no-key-message");
        List<LootEntry> loot = new ArrayList<>();
        List<?> rawLoot = yaml.getList("loot");
        if (rawLoot != null) {
            for (Object obj : rawLoot) {
                if (obj instanceof ConfigurationSection section) {
                    loot.add(readLootEntry(section));
                } else if (obj instanceof Map<?, ?> map) {
                    loot.add(readLootEntry(map));
                }
            }
        }
        return new CrateDefinition(
                id,
                displayName,
                displayBlock,
                broadcast,
                threshold,
                cooldown,
                noKey,
                autoKey,
                loot
        );
    }

    private AutoKeySettings loadAutoKey(YamlConfiguration yaml) {
        if (yaml.contains("auto-key.enabled") || yaml.contains("auto-key.permission") || yaml.contains("auto-key.group")) {
            return new AutoKeySettings(
                    yaml.getBoolean("auto-key.enabled", false),
                    yaml.getString("auto-key.group", ""),
                    yaml.getString("auto-key.permission", ""),
                    yaml.getLong("auto-key.interval-ms", 86_400_000L),
                    yaml.getInt("auto-key.amount", 1)
            );
        }
        // Migrate legacy permission-cooldown tier (first tier only) into auto-key timer
        boolean legacyEnabled = yaml.getBoolean("permission-cooldowns.enabled", false);
        ConfigurationSection tierSection = yaml.getConfigurationSection("permission-cooldowns.tiers");
        if (!legacyEnabled || tierSection == null || tierSection.getKeys(false).isEmpty()) {
            return new AutoKeySettings();
        }
        String permission = tierSection.getKeys(false).iterator().next();
        long intervalMs = tierSection.getLong(permission, 86_400_000L);
        return new AutoKeySettings(true, "", permission, intervalMs, 1);
    }

    private Material resolveDisplayBlock(YamlConfiguration yaml) {
        String displayBlock = yaml.getString("display-block");
        if (displayBlock != null) {
            Material material = Material.matchMaterial(displayBlock);
            if (material != null && BlockMaterialUtil.isValidDisplayBlock(material)) {
                return BlockMaterialUtil.normalizeDisplayBlock(material);
            }
        }
        String candleColor = yaml.getString("candle-color", "LIME_SHULKER_BOX");
        Material material = Material.matchMaterial(candleColor);
        if (material == null) {
            return Material.LIME_SHULKER_BOX;
        }
        return BlockMaterialUtil.normalizeDisplayBlock(material);
    }

    private LootEntry readLootEntry(ConfigurationSection section) {
        ItemStack item = section.getItemStack("item", new ItemStack(Material.STONE));
        int weight = section.getInt("weight", 1);
        return new LootEntry(item, weight);
    }

    @SuppressWarnings("unchecked")
    private LootEntry readLootEntry(Map<?, ?> map) {
        Object itemObj = map.get("item");
        ItemStack item;
        if (itemObj instanceof ItemStack stack) {
            item = stack;
        } else if (itemObj instanceof Map<?, ?> itemMap) {
            item = ItemStack.deserialize((Map<String, Object>) itemMap);
        } else {
            item = new ItemStack(Material.STONE);
        }
        int weight = map.get("weight") instanceof Number number ? number.intValue() : 1;
        return new LootEntry(item, weight);
    }
}
