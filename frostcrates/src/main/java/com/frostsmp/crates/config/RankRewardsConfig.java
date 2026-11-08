package com.frostsmp.crates.config;

import com.frostsmp.crates.FrostCratesPlugin;
import com.frostsmp.crates.model.KeyGrant;
import com.frostsmp.crates.model.RewardBundle;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public final class RankRewardsConfig {

    private final FrostCratesPlugin plugin;
    private boolean rankRewardsEnabled;
    private boolean boostRewardsEnabled;
    private List<RewardBundle> rankBundles = List.of();
    private List<RewardBundle> boostBundles = List.of();

    public RankRewardsConfig(FrostCratesPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        rankRewardsEnabled = plugin.getConfig().getBoolean("rank-rewards.enabled", true);
        boostRewardsEnabled = plugin.getConfig().getBoolean("boost-rewards.enabled", true);
        rankBundles = loadBundles(
                plugin.getConfig().getConfigurationSection("rank-rewards.ranks"),
                plugin.getConfig().getLong("rank-rewards.interval-hours", 24L) * 3_600_000L,
                true
        );
        boostBundles = loadBundles(
                plugin.getConfig().getConfigurationSection("boost-rewards.tiers"),
                plugin.getConfig().getLong("boost-rewards.interval-hours", 24L) * 3_600_000L,
                false
        );
    }

    private List<RewardBundle> loadBundles(ConfigurationSection section, long defaultIntervalMs, boolean rankSection) {
        if (section == null) {
            return List.of();
        }
        List<RewardBundle> bundles = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) {
                continue;
            }
            String display = entry.getString("display", id);
            String group = entry.getString("group", rankSection ? id : "");
            String permission = entry.getString("permission", rankSection ? "" : "frostsmp.crates.boost." + id.replace("boost", ""));
            long intervalMs = entry.getLong("interval-hours", defaultIntervalMs / 3_600_000L) * 3_600_000L;
            if (intervalMs <= 0L) {
                intervalMs = defaultIntervalMs;
            }
            List<KeyGrant> keys = loadKeys(entry.getConfigurationSection("keys"));
            RewardBundle bundle = new RewardBundle(id, display, group, permission, intervalMs, keys);
            if (bundle.isConfigured()) {
                bundles.add(bundle);
            } else {
                plugin.getLogger().warning("Skipping invalid reward bundle: " + id);
            }
        }
        return RewardBundle.unmodifiableList(bundles);
    }

    private List<KeyGrant> loadKeys(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }
        List<KeyGrant> keys = new ArrayList<>();
        for (String crateId : section.getKeys(false)) {
            int amount = section.getInt(crateId, 0);
            if (amount > 0) {
                keys.add(new KeyGrant(crateId.toLowerCase(), amount));
            }
        }
        return keys;
    }

    public boolean isRankRewardsEnabled() {
        return rankRewardsEnabled && !rankBundles.isEmpty();
    }

    public boolean isBoostRewardsEnabled() {
        return boostRewardsEnabled && !boostBundles.isEmpty();
    }

    public List<RewardBundle> getRankBundles() {
        return rankBundles;
    }

    public List<RewardBundle> getBoostBundles() {
        return boostBundles;
    }
}
