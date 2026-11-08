package com.frostsmp.chat.service;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Aggregates /stats data from FrostWallet, FrostCombat, and Bukkit statistics.
 * Soft-deps via reflection + YAML so FrostChat still works if either plugin is missing/outdated.
 */
public final class StatsService {

    public record PlayerStats(
            UUID playerId,
            String name,
            long wealth,
            boolean walletAvailable,
            int kills,
            int deaths,
            int killstreak,
            int bestKillstreak,
            boolean combatAvailable,
            long playtimeTicks
    ) {
        public double kdRatio() {
            if (deaths <= 0) {
                return kills;
            }
            return (double) kills / (double) deaths;
        }

        public String formattedPlaytime() {
            long totalSeconds = Math.max(0L, playtimeTicks) / 20L;
            long days = totalSeconds / 86_400L;
            long hours = (totalSeconds % 86_400L) / 3_600L;
            long minutes = (totalSeconds % 3_600L) / 60L;
            if (days > 0L) {
                return days + "d " + hours + "h " + minutes + "m";
            }
            if (hours > 0L) {
                return hours + "h " + minutes + "m";
            }
            return minutes + "m";
        }
    }

    private final JavaPlugin plugin;

    public StatsService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public PlayerStats load(OfflinePlayer player) {
        UUID id = player.getUniqueId();
        String name = player.getName() == null ? "Unknown" : player.getName();

        WealthResult wealth = loadWealth(id);
        StreakResult streaks = loadStreaks(id);

        int kills = safeStat(player, Statistic.PLAYER_KILLS);
        int deaths = safeStat(player, Statistic.DEATHS);
        long playtime = safeStatLong(player, Statistic.PLAY_ONE_MINUTE);

        return new PlayerStats(
                id,
                name,
                wealth.amount(),
                wealth.available(),
                kills,
                deaths,
                streaks.current(),
                streaks.best(),
                streaks.available(),
                playtime
        );
    }

    private WealthResult loadWealth(UUID playerId) {
        Plugin walletPlugin = plugin.getServer().getPluginManager().getPlugin("FrostWallet");
        if (walletPlugin == null || !walletPlugin.isEnabled()) {
            return WealthResult.missing();
        }
        try {
            Method getConfig = walletPlugin.getClass().getMethod("getPluginConfig");
            Method getService = walletPlugin.getClass().getMethod("getWalletService");
            Object config = getConfig.invoke(walletPlugin);
            Object service = getService.invoke(walletPlugin);
            if (config == null || service == null) {
                return loadWealthYaml(walletPlugin, playerId);
            }
            Method currenciesMethod = config.getClass().getMethod("currencies");
            @SuppressWarnings("unchecked")
            List<Object> currencies = (List<Object>) currenciesMethod.invoke(config);
            Method balanceMethod = findBalanceMethod(service.getClass());
            if (currencies == null || currencies.isEmpty() || balanceMethod == null) {
                return loadWealthYaml(walletPlugin, playerId);
            }
            long total = 0L;
            for (Object currency : currencies) {
                total += ((Number) balanceMethod.invoke(service, playerId, currency)).longValue();
            }
            return new WealthResult(true, total);
        } catch (Throwable ignored) {
            return loadWealthYaml(walletPlugin, playerId);
        }
    }

    private WealthResult loadWealthYaml(Plugin walletPlugin, UUID playerId) {
        File walletsFile = new File(walletPlugin.getDataFolder(), "wallets.yml");
        if (!walletsFile.exists()) {
            return new WealthResult(true, 0L);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(walletsFile);
        var section = yaml.getConfigurationSection("players." + playerId);
        if (section == null) {
            return new WealthResult(true, 0L);
        }
        long total = 0L;
        for (String key : section.getKeys(false)) {
            total += section.getLong(key, 0L);
        }
        return new WealthResult(true, total);
    }

    private StreakResult loadStreaks(UUID playerId) {
        Plugin combatPlugin = plugin.getServer().getPluginManager().getPlugin("FrostCombat");
        if (combatPlugin == null || !combatPlugin.isEnabled()) {
            return StreakResult.missing();
        }
        try {
            Method getter = combatPlugin.getClass().getMethod("getKillStreakService");
            Object service = getter.invoke(combatPlugin);
            if (service != null) {
                Method current = service.getClass().getMethod("current", UUID.class);
                Method best = service.getClass().getMethod("best", UUID.class);
                int cur = ((Number) current.invoke(service, playerId)).intValue();
                int bestVal = ((Number) best.invoke(service, playerId)).intValue();
                return new StreakResult(true, cur, bestVal);
            }
        } catch (Throwable ignored) {
            // Fall through to YAML.
        }
        return loadStreaksYaml(combatPlugin, playerId);
    }

    private StreakResult loadStreaksYaml(Plugin combatPlugin, UUID playerId) {
        File file = new File(combatPlugin.getDataFolder(), "killstreaks.yml");
        if (!file.exists()) {
            return new StreakResult(true, 0, 0);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String path = "players." + playerId;
        int current = yaml.getInt(path + ".current", 0);
        int best = yaml.getInt(path + ".best", 0);
        return new StreakResult(true, current, best);
    }

    private static Method findBalanceMethod(Class<?> type) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals("balance") || method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params[0] == UUID.class) {
                return method;
            }
        }
        return null;
    }

    private static int safeStat(OfflinePlayer player, Statistic statistic) {
        try {
            return player.getStatistic(statistic);
        } catch (IllegalArgumentException | UnsupportedOperationException exception) {
            return 0;
        }
    }

    private static long safeStatLong(OfflinePlayer player, Statistic statistic) {
        try {
            return player.getStatistic(statistic);
        } catch (IllegalArgumentException | UnsupportedOperationException exception) {
            return 0L;
        }
    }

    public static OfflinePlayer resolveTarget(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        var online = Bukkit.getPlayerExact(raw);
        if (online != null) {
            return online;
        }
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(raw)) {
                return offline;
            }
        }
        return Bukkit.getOfflinePlayer(raw);
    }

    public static String formatNumber(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    public static String formatRatio(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private record WealthResult(boolean available, long amount) {
        static WealthResult missing() {
            return new WealthResult(false, 0L);
        }
    }

    private record StreakResult(boolean available, int current, int best) {
        static StreakResult missing() {
            return new StreakResult(false, 0, 0);
        }
    }
}
