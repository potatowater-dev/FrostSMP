package com.frostsmp.wallet.service;

import com.frostsmp.wallet.config.PluginConfig;
import com.frostsmp.wallet.model.Currency;
import com.frostsmp.wallet.model.Denomination;
import com.frostsmp.wallet.storage.WalletStorage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WalletService {

    public record DepositResult(int items, long worth, Currency currency) {
    }

    public record WithdrawPlan(List<ItemStack> items) {
    }

    public record RankedBalance(UUID playerId, long amount, int rank) {
    }

    private static final long LEADERBOARD_TTL_MS = 2000L;

    private final PluginConfig config;
    private final WalletStorage storage;
    private final Object leaderboardLock = new Object();
    private final Map<String, LeaderboardSnapshot> snapshots = new HashMap<>();

    public WalletService(PluginConfig config, WalletStorage storage) {
        this.config = config;
        this.storage = storage;
    }

    public long balance(Player player, Currency currency) {
        return storage.get(player.getUniqueId(), currency.id());
    }

    public RankedBalance atRank(int place) {
        return atRank(place, defaultCurrencyId());
    }

    public RankedBalance atRank(int place, String currencyId) {
        LeaderboardSnapshot current = leaderboard(resolveCurrencyId(currencyId));
        if (place < 1 || place > current.ranked().size()) {
            return null;
        }
        return current.ranked().get(place - 1);
    }

    public RankedBalance rankOf(UUID playerId) {
        return rankOf(playerId, defaultCurrencyId());
    }

    public RankedBalance rankOf(UUID playerId, String currencyId) {
        if (playerId == null) {
            return null;
        }
        return leaderboard(resolveCurrencyId(currencyId)).byPlayer().get(playerId);
    }

    public long balance(UUID playerId) {
        return balance(playerId, defaultCurrency());
    }

    public long balance(UUID playerId, String currencyId) {
        Currency currency = config.currency(currencyId);
        if (currency == null) {
            currency = defaultCurrency();
        }
        return balance(playerId, currency);
    }

    public long balance(UUID playerId, Currency currency) {
        if (currency == null || playerId == null) {
            return 0L;
        }
        return storage.get(playerId, currency.id());
    }

    /** Wipes every player's wallet balances and invalidates the wealth leaderboard. */
    public void resetAll() {
        storage.clearAll();
        synchronized (leaderboardLock) {
            snapshots.clear();
        }
    }

    public String storedName(UUID playerId) {
        return storage.name(playerId);
    }

    private Currency defaultCurrency() {
        List<Currency> currencies = config.currencies();
        // Prefer gems for "wealth" defaults when both exist
        Currency gems = config.currency("gem");
        if (gems == null) {
            gems = config.currency("gems");
        }
        if (gems != null) {
            return gems;
        }
        return currencies.isEmpty() ? null : currencies.getFirst();
    }

    private String defaultCurrencyId() {
        Currency currency = defaultCurrency();
        return currency == null ? "" : currency.id();
    }

    private String resolveCurrencyId(String raw) {
        if (raw == null || raw.isBlank()) {
            return defaultCurrencyId();
        }
        String key = raw.toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
        return switch (key) {
            case "gem", "gems", "value", "money", "wealth" -> {
                Currency gems = config.currency("gem");
                if (gems == null) {
                    gems = config.currency("gems");
                }
                yield gems != null ? gems.id() : defaultCurrencyId();
            }
            case "kill_token", "killtoken", "kill_tokens", "token", "tokens" -> {
                Currency tokens = config.currency("kill_token");
                if (tokens == null) {
                    tokens = config.currency("killtoken");
                }
                yield tokens != null ? tokens.id() : key;
            }
            default -> {
                Currency direct = config.currency(key);
                yield direct != null ? direct.id() : key;
            }
        };
    }

    private LeaderboardSnapshot leaderboard(String currencyId) {
        long now = System.currentTimeMillis();
        String id = currencyId == null ? "" : currencyId;
        synchronized (leaderboardLock) {
            LeaderboardSnapshot current = snapshots.get(id);
            if (current != null
                    && current.builtAt() + LEADERBOARD_TTL_MS > now
                    && current.currencyId().equals(id)) {
                return current;
            }
            LeaderboardSnapshot built = buildLeaderboard(id);
            snapshots.put(id, built);
            return built;
        }
    }

    private LeaderboardSnapshot buildLeaderboard(String currencyId) {
        if (currencyId.isEmpty()) {
            return new LeaderboardSnapshot(currencyId, System.currentTimeMillis(), List.of(), Map.of());
        }
        List<Map.Entry<UUID, Long>> amounts = storage.amounts(currencyId);
        amounts.sort(Comparator
                .comparingLong((Map.Entry<UUID, Long> entry) -> entry.getValue())
                .reversed()
                .thenComparing(entry -> entry.getKey()));
        List<RankedBalance> ranked = new ArrayList<>(amounts.size());
        Map<UUID, RankedBalance> byPlayer = new HashMap<>();
        int place = 1;
        for (Map.Entry<UUID, Long> entry : amounts) {
            RankedBalance row = new RankedBalance(entry.getKey(), entry.getValue(), place++);
            ranked.add(row);
            byPlayer.put(row.playerId(), row);
        }
        return new LeaderboardSnapshot(
                currencyId,
                System.currentTimeMillis(),
                List.copyOf(ranked),
                Map.copyOf(byPlayer));
    }

    private record LeaderboardSnapshot(
            String currencyId,
            long builtAt,
            List<RankedBalance> ranked,
            Map<UUID, RankedBalance> byPlayer
    ) {
        static final LeaderboardSnapshot EMPTY = new LeaderboardSnapshot("", 0L, List.of(), Map.of());
    }

    public DepositResult depositHand(Player player, int amount) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        Currency currency = config.currencyOf(hand);
        if (currency == null) {
            return null;
        }
        Denomination denomination = currency.match(hand);
        if (denomination == null) {
            return null;
        }
        int take = Math.min(amount, hand.getAmount());
        if (take <= 0) {
            return new DepositResult(0, 0L, currency);
        }
        long worth = denomination.worth() * take;
        storage.set(player.getUniqueId(), currency.id(), balance(player, currency) + worth, player.getName());
        int left = hand.getAmount() - take;
        if (left <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(left);
        }
        return new DepositResult(take, worth, currency);
    }

    public List<DepositResult> depositAll(Player player, Currency filter) {
        List<Currency> targets = filter == null ? config.currencies() : List.of(filter);
        List<DepositResult> results = new ArrayList<>();
        for (Currency currency : targets) {
            DepositResult part = depositAllFor(player, currency);
            if (part.items() > 0) {
                results.add(part);
            }
        }
        return results;
    }

    private DepositResult depositAllFor(Player player, Currency currency) {
        PlayerInventory inventory = player.getInventory();
        int items = 0;
        long worth = 0L;
        for (int slot = 0; slot < inventory.getStorageContents().length; slot++) {
            ItemStack stack = inventory.getItem(slot);
            Denomination denomination = currency.match(stack);
            if (denomination == null) {
                continue;
            }
            items += stack.getAmount();
            worth += denomination.worth() * stack.getAmount();
            inventory.setItem(slot, null);
        }
        ItemStack offhand = inventory.getItemInOffHand();
        Denomination offDenom = currency.match(offhand);
        if (offDenom != null) {
            items += offhand.getAmount();
            worth += offDenom.worth() * offhand.getAmount();
            inventory.setItemInOffHand(null);
        }
        if (items == 0) {
            return new DepositResult(0, 0L, currency);
        }
        storage.set(player.getUniqueId(), currency.id(), balance(player, currency) + worth, player.getName());
        return new DepositResult(items, worth, currency);
    }

    public String withdraw(Player player, Currency currency, long amount) {
        if (amount <= 0L) {
            return "invalid-amount";
        }
        long current = balance(player, currency);
        if (current < amount) {
            return "not-enough-balance";
        }
        WithdrawPlan plan = planWithdraw(currency, amount);
        if (plan == null) {
            return "cannot-make-amount";
        }
        storage.set(player.getUniqueId(), currency.id(), current - amount, player.getName());
        giveOrDrop(player, plan.items());
        return "ok";
    }

    private WithdrawPlan planWithdraw(Currency currency, long amount) {
        long remaining = amount;
        List<ItemStack> items = new ArrayList<>();
        for (Denomination denomination : currency.denominations()) {
            if (denomination.worth() <= 0L || remaining < denomination.worth()) {
                continue;
            }
            long count = remaining / denomination.worth();
            remaining -= count * denomination.worth();
            while (count > 0L) {
                int maxStack = Math.max(1, denomination.template().getMaxStackSize());
                int stackSize = (int) Math.min(maxStack, count);
                ItemStack stack = denomination.template().clone();
                stack.setAmount(stackSize);
                items.add(stack);
                count -= stackSize;
            }
        }
        if (remaining != 0L) {
            return null;
        }
        return new WithdrawPlan(items);
    }

    private static void giveOrDrop(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (leftover.isEmpty()) {
                continue;
            }
            Location loc = player.getLocation();
            if (loc.getWorld() == null) {
                continue;
            }
            for (ItemStack drop : leftover.values()) {
                loc.getWorld().dropItemNaturally(loc, drop);
            }
        }
    }
}
