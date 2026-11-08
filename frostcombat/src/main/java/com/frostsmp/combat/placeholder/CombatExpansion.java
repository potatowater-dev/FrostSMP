package com.frostsmp.combat.placeholder;

import com.frostsmp.combat.service.KillStreakService;
import com.frostsmp.combat.service.PlayerStatsService;
import com.frostsmp.combat.service.PlayerStatsService.Metric;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Killstreak + asleaderboard-style kills/deaths/playtime placeholders (hologram-safe).
 */
public final class CombatExpansion extends PlaceholderExpansion {

    private static final Pattern TOP_STREAK_PLAYER = Pattern.compile("^top_(\\d+)_killstreak_player$");
    private static final Pattern TOP_STREAK_VALUE = Pattern.compile("^top_(\\d+)_have_killstreak$");

    private static final Pattern TOP_STAT_PLAYER = Pattern.compile(
            "^top_(\\d+)_(kills|kill|deaths|death|playtime)_player$");
    private static final Pattern TOP_STAT_VALUE = Pattern.compile(
            "^top_(\\d+)_have_(kills|kill|deaths|death|playtime)$");

    private final JavaPlugin plugin;
    private final KillStreakService streaks;
    private final PlayerStatsService stats;

    public CombatExpansion(JavaPlugin plugin, KillStreakService streaks, PlayerStatsService stats) {
        this.plugin = plugin;
        this.streaks = streaks;
        this.stats = stats;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "frostcombat";
    }

    @Override
    public @NotNull String getAuthor() {
        return "FrostSMP";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        if (params.isEmpty()) {
            return "";
        }
        String key = params.toLowerCase(Locale.ROOT).trim();

        Matcher streakPlayer = TOP_STREAK_PLAYER.matcher(key);
        if (streakPlayer.matches()) {
            KillStreakService.Entry entry = streaks.top(Integer.parseInt(streakPlayer.group(1)));
            return entry == null ? "-" : entry.name();
        }
        Matcher streakValue = TOP_STREAK_VALUE.matcher(key);
        if (streakValue.matches()) {
            KillStreakService.Entry entry = streaks.top(Integer.parseInt(streakValue.group(1)));
            return entry == null ? "0" : String.valueOf(entry.streak());
        }

        Matcher topPlayer = TOP_STAT_PLAYER.matcher(key);
        if (topPlayer.matches()) {
            Metric metric = metricOf(topPlayer.group(2));
            PlayerStatsService.Entry entry = stats.top(metric, Integer.parseInt(topPlayer.group(1)));
            return entry == null ? "-" : entry.name();
        }
        Matcher topValue = TOP_STAT_VALUE.matcher(key);
        if (topValue.matches()) {
            Metric metric = metricOf(topValue.group(2));
            PlayerStatsService.Entry entry = stats.top(metric, Integer.parseInt(topValue.group(1)));
            return entry == null
                    ? (metric == Metric.PLAYTIME ? "0m" : "0")
                    : PlayerStatsService.formatValue(metric, entry.value());
        }

        UUID id = player == null ? null : player.getUniqueId();
        return switch (key) {
            case "killstreak", "ks" -> id == null ? "0" : String.valueOf(streaks.current(id));
            case "best_killstreak", "best_ks" -> id == null ? "0" : String.valueOf(streaks.best(id));
            case "place", "rank", "position" -> {
                if (id == null) {
                    yield "-";
                }
                int place = streaks.place(id);
                yield place <= 0 ? "-" : String.valueOf(place);
            }
            case "kills", "value_kills", "value_kill" -> id == null ? "0" : formatStat(Metric.KILLS, id);
            case "deaths", "death", "value_deaths", "value_death" -> id == null ? "0" : formatStat(Metric.DEATHS, id);
            case "playtime", "value_playtime" -> id == null ? "0m" : formatStat(Metric.PLAYTIME, id);
            case "rank_kills", "place_kills", "rank_kill", "place_kill" -> placeOf(Metric.KILLS, id);
            case "rank_deaths", "place_deaths", "rank_death", "place_death" -> placeOf(Metric.DEATHS, id);
            case "rank_playtime", "place_playtime" -> placeOf(Metric.PLAYTIME, id);
            default -> null;
        };
    }

    @Override
    public @Nullable String onPlaceholderRequest(@Nullable Player player, @NotNull String params) {
        return onRequest(player, params);
    }

    private String formatStat(Metric metric, UUID id) {
        return PlayerStatsService.formatValue(metric, stats.value(metric, id));
    }

    private String placeOf(Metric metric, UUID id) {
        if (id == null) {
            return "-";
        }
        int place = stats.place(metric, id);
        return place <= 0 ? "-" : String.valueOf(place);
    }

    private static Metric metricOf(String raw) {
        return switch (raw) {
            case "deaths", "death" -> Metric.DEATHS;
            case "playtime" -> Metric.PLAYTIME;
            default -> Metric.KILLS;
        };
    }
}
