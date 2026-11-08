package com.frost.plots.config;

import com.frost.plots.FrostPlotsPlugin;
import com.frost.plots.hook.ItemCostHook;
import com.frost.plots.model.UpgradeTier;
import com.frost.plots.util.MessageUtil;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PluginConfig {

    private final FrostPlotsPlugin plugin;
    private String prefix;
    private String plotWorld;
    private int gridSpacing;
    private int platformY;
    private int maxMembers;
    private int listPageSize;
    private int deleteConfirmSeconds;
    private int regionHeight;
    private boolean allowBoundaryBypass;
    private List<UpgradeTier> upgradeTiers = List.of();

    public PluginConfig(FrostPlotsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        prefix = plugin.getConfig().getString("prefix", "<gray>[<aqua>FrostPlots<gray>] ");
        plotWorld = plugin.getConfig().getString("plot-world", "plots");
        gridSpacing = Math.max(16, plugin.getConfig().getInt("grid-spacing", 200));
        platformY = plugin.getConfig().getInt("platform-y", 64);
        maxMembers = Math.max(1, plugin.getConfig().getInt("max-members", 16));
        listPageSize = Math.max(1, plugin.getConfig().getInt("list-page-size", 8));
        deleteConfirmSeconds = Math.max(5, plugin.getConfig().getInt("delete-confirm-seconds", 15));
        regionHeight = Math.max(8, plugin.getConfig().getInt("region-height", 80));
        allowBoundaryBypass = plugin.getConfig().getBoolean("allow-boundary-bypass", false);

        List<UpgradeTier> tiers = new ArrayList<>();
        var list = plugin.getConfig().getList("upgrade-tiers");
        if (list != null) {
            for (Object entry : list) {
                if (!(entry instanceof Map<?, ?> map)) {
                    continue;
                }
                int size = toInt(map.get("size"), 10);
                int costAmount = toInt(map.get("cost-amount"), 0);
                String costItem = map.get("cost-item") == null ? "" : String.valueOf(map.get("cost-item"));
                if (size < 1) {
                    continue;
                }
                ItemStack template = ItemCostHook.parse(plugin, costItem);
                tiers.add(new UpgradeTier(size, Math.max(0, costAmount), costItem, template));
            }
        }
        if (tiers.isEmpty()) {
            tiers.add(new UpgradeTier(10, 0, "", null));
        }
        this.upgradeTiers = List.copyOf(tiers);
    }

    public boolean saveUpgradeCost(int tierIndex, int amount, ItemStack item) {
        if (tierIndex < 0 || tierIndex >= upgradeTiers.size()) {
            return false;
        }
        String snbt = ItemCostHook.toSnbt(item);
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < upgradeTiers.size(); i++) {
            UpgradeTier tier = upgradeTiers.get(i);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("size", tier.size());
            if (i == tierIndex) {
                map.put("cost-amount", Math.max(0, amount));
                map.put("cost-item", snbt);
            } else {
                map.put("cost-amount", tier.costAmount());
                map.put("cost-item", tier.costItem() == null ? "" : tier.costItem());
            }
            list.add(map);
        }
        plugin.getConfig().set("upgrade-tiers", list);
        plugin.saveConfig();
        load();
        return true;
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public String prefix() {
        return prefix;
    }

    public String plotWorld() {
        return plotWorld;
    }

    public int gridSpacing() {
        return gridSpacing;
    }

    public int platformY() {
        return platformY;
    }

    public int maxMembers() {
        return maxMembers;
    }

    public int listPageSize() {
        return listPageSize;
    }

    public int deleteConfirmSeconds() {
        return deleteConfirmSeconds;
    }

    public int regionHeight() {
        return regionHeight;
    }

    public boolean allowBoundaryBypass() {
        return allowBoundaryBypass;
    }

    public List<UpgradeTier> upgradeTiers() {
        return upgradeTiers;
    }

    public UpgradeTier tier(int index) {
        if (index < 0 || index >= upgradeTiers.size()) {
            return upgradeTiers.get(upgradeTiers.size() - 1);
        }
        return upgradeTiers.get(index);
    }

    public boolean hasNextTier(int index) {
        return index + 1 < upgradeTiers.size();
    }

    public String message(String key) {
        return plugin.getConfig().getString("messages." + key, "<red>Missing message: " + key + "</red>");
    }

    public String prefixed(String key, String... replacements) {
        return prefix + MessageUtil.format(message(key), replacements);
    }
}
