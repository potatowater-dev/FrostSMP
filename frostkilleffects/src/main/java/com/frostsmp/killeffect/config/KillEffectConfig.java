package com.frostsmp.killeffect.config;

import com.frostsmp.killeffect.item.KillEffectItem;
import com.frostsmp.killeffect.model.KillEffectDefinition;
import com.frostsmp.killeffect.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KillEffectConfig {

    private final JavaPlugin plugin;
    private final Map<String, KillEffectDefinition> effects = new LinkedHashMap<>();

    public KillEffectConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        effects.clear();

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("effects");
        if (root == null) {
            plugin.getLogger().warning("No effects configured.");
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            String key = id.toLowerCase(Locale.ROOT);
            String name = section.getString("name", id);
            String permission = section.getString("permission", "frostsmp.killeffect.claim." + key);
            ItemStack claimItem = loadItem(section, key);
            if (claimItem == null) {
                plugin.getLogger().warning("Effect " + key + " has no valid claim item.");
                continue;
            }
            claimItem = KillEffectItem.stamp(claimItem, key);
            claimItem.setAmount(1);
            effects.put(key, new KillEffectDefinition(key, name, permission, claimItem));
        }
    }

    private ItemStack loadItem(ConfigurationSection section, String id) {
        ItemStack stacked = section.getItemStack("item-data");
        if (stacked != null && !stacked.getType().isAir()) {
            return stacked;
        }
        String giveString = section.getString("item", "");
        if (giveString == null || giveString.isBlank()) {
            return null;
        }
        try {
            return Bukkit.getItemFactory().createItemStack(giveString);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Bad item for " + id + ": " + exception.getMessage());
            return null;
        }
    }

    public void saveClaimItem(String effectId, ItemStack item) {
        String key = effectId.toLowerCase(Locale.ROOT);
        ItemStack copy = KillEffectItem.stamp(item, key);
        copy.setAmount(1);
        plugin.getConfig().set("effects." + key + ".item-data", copy);
        plugin.getConfig().set("effects." + key + ".item", null);
        plugin.saveConfig();
        load();
    }

    public List<KillEffectDefinition> effects() {
        return List.copyOf(effects.values());
    }

    public KillEffectDefinition effect(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return effects.get(raw.toLowerCase(Locale.ROOT).replace(' ', '_'));
    }

    public KillEffectDefinition effectOf(ItemStack item) {
        String tagged = KillEffectItem.effectId(item);
        if (tagged != null) {
            KillEffectDefinition byTag = effects.get(tagged.toLowerCase(Locale.ROOT));
            if (byTag != null) {
                return byTag;
            }
        }
        for (KillEffectDefinition effect : effects.values()) {
            if (effect.matches(item)) {
                return effect;
            }
        }
        return null;
    }

    public List<String> effectIds() {
        return new ArrayList<>(effects.keySet());
    }

    public String prefixed(String key, String... kv) {
        return plugin.getConfig().getString("messages.prefix", "") + MessageUtil.format(msg(key), kv);
    }

    public String msg(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }
}
