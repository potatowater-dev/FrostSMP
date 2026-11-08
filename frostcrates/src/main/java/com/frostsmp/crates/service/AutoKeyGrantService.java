package com.frostsmp.crates.service;

import com.frostsmp.crates.FrostCratesPlugin;
import com.frostsmp.crates.config.AutoKeyGrantStorage;
import com.frostsmp.crates.config.CrateDefinitionStorage;
import com.frostsmp.crates.config.MessagesConfig;
import com.frostsmp.crates.config.PluginConfig;
import com.frostsmp.crates.config.RankRewardsConfig;
import com.frostsmp.crates.model.AutoKeySettings;
import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.model.KeyGrant;
import com.frostsmp.crates.model.RewardBundle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AutoKeyGrantService {

    public record PendingGrant(UUID playerId, String crateId, int amount, long lastGrantMs) {
    }

    public record PendingBundleGrant(
            UUID playerId,
            String category,
            RewardBundle bundle,
            List<KeyGrant> keys,
            long lastGrantMs
    ) {
    }

    private final FrostCratesPlugin plugin;
    private final PluginConfig config;
    private final RankRewardsConfig rankRewardsConfig;
    private final MessagesConfig messages;
    private final CrateDefinitionStorage definitions;
    private final AutoKeyGrantStorage grantStorage;
    private final PermissionLookupService permissionLookup;
    private final KeyService keyService;
    private final HologramService hologramService;
    private BukkitTask task;

    public AutoKeyGrantService(
            FrostCratesPlugin plugin,
            PluginConfig config,
            RankRewardsConfig rankRewardsConfig,
            MessagesConfig messages,
            CrateDefinitionStorage definitions,
            AutoKeyGrantStorage grantStorage,
            PermissionLookupService permissionLookup,
            KeyService keyService,
            HologramService hologramService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.rankRewardsConfig = rankRewardsConfig;
        this.messages = messages;
        this.definitions = definitions;
        this.grantStorage = grantStorage;
        this.permissionLookup = permissionLookup;
        this.keyService = keyService;
        this.hologramService = hologramService;
    }

    public void start() {
        stop();
        long intervalTicks = Math.max(20L, config.getAutoKeyCheckIntervalSeconds() * 20L);
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::runCheckAsync, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void removeCrate(String crateId) {
        grantStorage.removeCrate(crateId);
    }

    private void runCheckAsync() {
        long now = System.currentTimeMillis();
        List<PendingGrant> grants = new ArrayList<>();
        List<PendingBundleGrant> bundleGrants = new ArrayList<>();
        for (CrateDefinition definition : definitions.getAll()) {
            AutoKeySettings autoKey = definition.getAutoKey();
            if (!autoKey.isConfigured()) {
                continue;
            }
            Set<UUID> players = permissionLookup.findEligiblePlayers(autoKey);
            for (UUID playerId : players) {
                PendingGrant grant = computeGrant(playerId, definition, autoKey, now);
                if (grant != null) {
                    grants.add(grant);
                }
            }
        }
        collectBundleGrants(now, bundleGrants);
        if (grants.isEmpty() && bundleGrants.isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            applyGrants(grants);
            applyBundleGrants(bundleGrants);
        });
    }

    private void collectBundleGrants(long now, List<PendingBundleGrant> bundleGrants) {
        Set<UUID> players = permissionLookup.getAllTrackedPlayerIds();
        if (rankRewardsConfig.isRankRewardsEnabled()) {
            for (UUID playerId : players) {
                findHighestBundle(playerId, rankRewardsConfig.getRankBundles())
                        .flatMap(bundle -> computeBundleGrant(playerId, "rank", bundle, now))
                        .ifPresent(bundleGrants::add);
            }
        }
        if (rankRewardsConfig.isBoostRewardsEnabled()) {
            for (UUID playerId : players) {
                findHighestBundle(playerId, rankRewardsConfig.getBoostBundles())
                        .flatMap(bundle -> computeBundleGrant(playerId, "boost", bundle, now))
                        .ifPresent(bundleGrants::add);
            }
        }
    }

    private Optional<RewardBundle> findHighestBundle(UUID playerId, List<RewardBundle> bundles) {
        for (int index = bundles.size() - 1; index >= 0; index--) {
            RewardBundle bundle = bundles.get(index);
            if (bundle.matches(permissionLookup, playerId)) {
                return Optional.of(bundle);
            }
        }
        return Optional.empty();
    }

    private Optional<PendingBundleGrant> computeBundleGrant(
            UUID playerId,
            String category,
            RewardBundle bundle,
            long now
    ) {
        long intervalMs = bundle.getIntervalMs();
        if (intervalMs <= 0L) {
            return Optional.empty();
        }
        long lastGrant = grantStorage.getLastBundleGrantMs(playerId, category, bundle.getId());
        if (lastGrant <= 0L) {
            return Optional.of(new PendingBundleGrant(playerId, category, bundle, bundle.getKeys(), now));
        }
        long elapsed = now - lastGrant;
        if (elapsed < intervalMs) {
            return Optional.empty();
        }
        int periods = (int) (elapsed / intervalMs);
        int capped = Math.min(periods, config.getAutoKeyMaxCatchup());
        long newLastGrant = lastGrant + (long) capped * intervalMs;
        return Optional.of(new PendingBundleGrant(
                playerId,
                category,
                bundle,
                bundle.scaledKeys(capped),
                newLastGrant
        ));
    }

    private PendingGrant computeGrant(UUID playerId, CrateDefinition definition, AutoKeySettings autoKey, long now) {
        long intervalMs = autoKey.getIntervalMs();
        if (intervalMs <= 0L) {
            return null;
        }
        long lastGrant = grantStorage.getLastGrantMs(playerId, definition.getId());
        if (lastGrant <= 0L) {
            return new PendingGrant(playerId, definition.getId(), autoKey.getAmount(), now);
        }
        long elapsed = now - lastGrant;
        if (elapsed < intervalMs) {
            return null;
        }
        int periods = (int) (elapsed / intervalMs);
        int capped = Math.min(periods, config.getAutoKeyMaxCatchup());
        long newLastGrant = lastGrant + (long) capped * intervalMs;
        return new PendingGrant(playerId, definition.getId(), capped * autoKey.getAmount(), newLastGrant);
    }

    private void applyGrants(List<PendingGrant> grants) {
        for (PendingGrant grant : grants) {
            keyService.give(grant.playerId(), grant.crateId(), grant.amount());
            grantStorage.setLastGrantMs(grant.playerId(), grant.crateId(), grant.lastGrantMs());
            if (!config.isAutoKeyNotifyOnline()) {
                continue;
            }
            Player player = Bukkit.getPlayer(grant.playerId());
            if (player == null || !player.isOnline()) {
                continue;
            }
            CrateDefinition definition = definitions.get(grant.crateId());
            String crateName = definition != null ? definition.getDisplayName() : grant.crateId();
            messages.send(player, "auto-key-received", "crate", crateName, "amount", String.valueOf(grant.amount()));
            hologramService.refreshKeysFor(player);
        }
        grantStorage.save();
        keyService.flush();
    }

    private void applyBundleGrants(List<PendingBundleGrant> grants) {
        for (PendingBundleGrant grant : grants) {
            for (KeyGrant keyGrant : grant.keys()) {
                keyService.give(grant.playerId(), keyGrant.crateId(), keyGrant.amount());
            }
            grantStorage.setLastBundleGrantMs(
                    grant.playerId(),
                    grant.category(),
                    grant.bundle().getId(),
                    grant.lastGrantMs()
            );
            if (!config.isAutoKeyNotifyOnline()) {
                continue;
            }
            Player player = Bukkit.getPlayer(grant.playerId());
            if (player == null || !player.isOnline()) {
                continue;
            }
            String messageKey = "rank".equals(grant.category())
                    ? "rank-bundle-received"
                    : "boost-bundle-received";
            messages.send(
                    player,
                    messageKey,
                    "rank",
                    grant.bundle().getDisplayName(),
                    "details",
                    formatBundleDetails(grant.keys())
            );
            hologramService.refreshKeysFor(player);
        }
        if (!grants.isEmpty()) {
            grantStorage.save();
            keyService.flush();
        }
    }

    private String formatBundleDetails(List<KeyGrant> keys) {
        List<String> parts = new ArrayList<>();
        for (KeyGrant grant : keys) {
            CrateDefinition definition = definitions.get(grant.crateId());
            String crateName = definition != null ? definition.getDisplayName() : grant.crateId();
            parts.add(grant.amount() + " " + crateName);
        }
        return String.join(", ", parts);
    }
}
