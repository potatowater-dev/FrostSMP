package com.frostsmp.wallet.placeholder;

import com.frostsmp.wallet.service.WalletService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
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
 * asleaderboard-style: {@code %frostwallet_top_4_gem_player%} / {@code %frostwallet_top_4_have_gem%}
 * Kill tokens: {@code %frostwallet_top_1_kill_token_player%} / {@code %frostwallet_top_1_have_kill_token%}
 */
public final class WalletExpansion extends PlaceholderExpansion {

    private static final Pattern TOP_PLAYER = Pattern.compile(
            "^top_(\\d+)_(gem|gems|value|money|kill_token|killtoken|kill_tokens|token|tokens)_player$");
    private static final Pattern TOP_HAVE = Pattern.compile(
            "^top_(\\d+)_have_(gem|gems|value|money|kill_token|killtoken|kill_tokens|token|tokens)$");
    private static final Pattern RANK_FIELD = Pattern.compile(
            "^rank(?:_?)(\\d+)(?:_(player|money|value|gem|gems|kill_token|killtoken))?$");

    private final JavaPlugin plugin;
    private final WalletService wallet;

    public WalletExpansion(JavaPlugin plugin, WalletService wallet) {
        this.plugin = plugin;
        this.wallet = wallet;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "frostwallet";
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

        Matcher topPlayer = TOP_PLAYER.matcher(key);
        if (topPlayer.matches()) {
            return playerAt(Integer.parseInt(topPlayer.group(1)), topPlayer.group(2));
        }
        Matcher topHave = TOP_HAVE.matcher(key);
        if (topHave.matches()) {
            return amountAt(Integer.parseInt(topHave.group(1)), topHave.group(2));
        }

        Matcher rank = RANK_FIELD.matcher(key);
        if (rank.matches()) {
            int place = Integer.parseInt(rank.group(1));
            String field = rank.group(2) == null ? "player" : rank.group(2);
            return switch (field) {
                case "money", "value", "gem", "gems" -> amountAt(place, "gem");
                case "kill_token", "killtoken" -> amountAt(place, "kill_token");
                default -> playerAt(place, "gem");
            };
        }

        UUID id = player == null ? null : player.getUniqueId();
        return switch (key) {
            case "rank", "place", "position", "rank_gem", "rank_gems" -> personalRank(id, "gem");
            case "rank_kill_token", "rank_killtoken", "rank_token" -> personalRank(id, "kill_token");
            case "value", "money", "gem", "gems", "balance" -> format(wallet.balance(id, "gem"));
            case "kill_token", "killtoken", "tokens" -> format(wallet.balance(id, "kill_token"));
            default -> null;
        };
    }

    @Override
    public @Nullable String onPlaceholderRequest(@Nullable Player player, @NotNull String params) {
        return onRequest(player, params);
    }

    private String personalRank(UUID playerId, String currencyKey) {
        if (playerId == null) {
            return "-";
        }
        WalletService.RankedBalance row = wallet.rankOf(playerId, currencyKey);
        return row == null ? "-" : String.valueOf(row.rank());
    }

    private String playerAt(int place, String currencyKey) {
        if (place < 1) {
            return "-";
        }
        WalletService.RankedBalance row = wallet.atRank(place, currencyKey);
        return row == null ? "-" : nameOf(wallet, row.playerId());
    }

    private String amountAt(int place, String currencyKey) {
        if (place < 1) {
            return "0";
        }
        WalletService.RankedBalance row = wallet.atRank(place, currencyKey);
        return row == null ? "0" : format(row.amount());
    }

    private static String nameOf(WalletService wallet, UUID playerId) {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            return online.getName();
        }
        String stored = wallet.storedName(playerId);
        if (stored != null && !stored.isBlank()) {
            return stored;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        String name = offline.getName();
        return name == null || name.isBlank() ? "Unknown" : name;
    }

    private static String format(long amount) {
        return Long.toString(amount);
    }
}
