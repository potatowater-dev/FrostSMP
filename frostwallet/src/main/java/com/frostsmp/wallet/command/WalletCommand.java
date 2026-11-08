package com.frostsmp.wallet.command;

import com.frostsmp.wallet.FrostWalletPlugin;
import com.frostsmp.wallet.config.PluginConfig;
import com.frostsmp.wallet.model.Currency;
import com.frostsmp.wallet.service.WalletService;
import com.frostsmp.wallet.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class WalletCommand implements CommandExecutor, TabCompleter {

    private static final String USE = "frostsmp.wallet.use";
    private static final String ADMIN = "frostsmp.wallet.admin";

    private final FrostWalletPlugin plugin;
    private final PluginConfig config;
    private final WalletService wallet;

    public WalletCommand(FrostWalletPlugin plugin, PluginConfig config, WalletService wallet) {
        this.plugin = plugin;
        this.config = config;
        this.wallet = wallet;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("frostwallet")) {
            name = "wallet";
        }
        if (name.equals("bal")) {
            name = "balance";
        }
        if (name.equals("dep")) {
            name = "deposit";
        }
        if (name.equals("with")) {
            name = "withdraw";
        }

        if (name.equals("wallet") && args.length > 0) {
            String adminSub = args[0].toLowerCase(Locale.ROOT);
            if (adminSub.equals("reload") || adminSub.equals("reset") || adminSub.equals("resetall")) {
                if (!sender.hasPermission(ADMIN)) {
                    MessageUtil.send(sender, config.prefixed("no-permission"));
                    return true;
                }
                if (adminSub.equals("reload")) {
                    plugin.reloadAll();
                    MessageUtil.send(sender, config.prefixed("reloaded"));
                    return true;
                }
                wallet.resetAll();
                MessageUtil.send(sender, config.prefixed("reset-all"));
                return true;
            }
        }

        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.prefixed("players-only"));
            return true;
        }
        if (!player.hasPermission(USE)) {
            MessageUtil.send(player, config.prefixed("no-permission"));
            return true;
        }

        return switch (name) {
            case "balance" -> showBalance(player);
            case "deposit" -> deposit(player, args, 0);
            case "withdraw" -> withdraw(player, args, 0);
            case "depositall" -> depositAll(player, args);
            case "wallet" -> walletRoot(player, args);
            default -> true;
        };
    }

    private boolean walletRoot(Player player, String[] args) {
        if (args.length == 0) {
            return showBalance(player);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "deposit", "dep" -> deposit(player, args, 1);
            case "withdraw", "with" -> withdraw(player, args, 1);
            default -> {
                MessageUtil.send(player, config.prefixed("usage-wallet"));
                yield true;
            }
        };
    }

    private boolean showBalance(Player player) {
        List<Currency> currencies = config.currencies();
        if (currencies.isEmpty()) {
            MessageUtil.send(player, config.prefixed("balance-empty"));
            return true;
        }
        MessageUtil.send(player, config.prefixed("balance-header"));
        for (Currency currency : currencies) {
            long amount = wallet.balance(player, currency);
            MessageUtil.send(player, config.prefixed("balance-line",
                    "currency", currency.name(),
                    "balance", format(amount)));
        }
        return true;
    }

    private boolean deposit(Player player, String[] args, int offset) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            MessageUtil.send(player, config.prefixed("empty-hand"));
            return true;
        }
        if (config.currencyOf(hand) == null) {
            MessageUtil.send(player, config.prefixed("not-currency"));
            return true;
        }
        int amount = hand.getAmount();
        if (args.length > offset) {
            Long parsed = parseAmount(args[offset]);
            if (parsed == null) {
                MessageUtil.send(player, config.prefixed("invalid-amount"));
                return true;
            }
            if (parsed > Integer.MAX_VALUE) {
                MessageUtil.send(player, config.prefixed("invalid-amount"));
                return true;
            }
            amount = parsed.intValue();
        }
        if (amount > hand.getAmount()) {
            MessageUtil.send(player, config.prefixed("not-enough-items"));
            return true;
        }
        WalletService.DepositResult result = wallet.depositHand(player, amount);
        if (result == null || result.items() <= 0) {
            MessageUtil.send(player, config.prefixed("not-currency"));
            return true;
        }
        MessageUtil.send(player, config.prefixed("deposited",
                "items", String.valueOf(result.items()),
                "worth", format(result.worth()),
                "currency", result.currency().name()));
        return true;
    }

    private boolean withdraw(Player player, String[] args, int offset) {
        if (args.length < offset + 2) {
            MessageUtil.send(player, config.prefixed("usage-withdraw"));
            return true;
        }
        Currency currency = config.currency(args[offset]);
        if (currency == null) {
            MessageUtil.send(player, config.prefixed("unknown-currency", "currency", args[offset]));
            return true;
        }
        Long amount = parseAmount(args[offset + 1]);
        if (amount == null) {
            MessageUtil.send(player, config.prefixed("invalid-amount"));
            return true;
        }
        String result = wallet.withdraw(player, currency, amount);
        switch (result) {
            case "not-enough-balance" -> MessageUtil.send(player, config.prefixed("not-enough-balance",
                    "currency", currency.name(),
                    "balance", format(wallet.balance(player, currency))));
            case "cannot-make-amount" -> MessageUtil.send(player, config.prefixed("cannot-make-amount",
                    "amount", format(amount),
                    "currency", currency.name()));
            case "invalid-amount" -> MessageUtil.send(player, config.prefixed("invalid-amount"));
            default -> MessageUtil.send(player, config.prefixed("withdrawn",
                    "amount", format(amount),
                    "currency", currency.name()));
        }
        return true;
    }

    private boolean depositAll(Player player, String[] args) {
        Currency filter = null;
        if (args.length >= 1) {
            filter = config.currency(args[0]);
            if (filter == null) {
                MessageUtil.send(player, config.prefixed("unknown-currency", "currency", args[0]));
                return true;
            }
        }
        List<WalletService.DepositResult> results = wallet.depositAll(player, filter);
        if (results.isEmpty()) {
            MessageUtil.send(player, config.prefixed("deposited-none"));
            return true;
        }
        for (WalletService.DepositResult result : results) {
            MessageUtil.send(player, config.prefixed("deposited",
                    "items", String.valueOf(result.items()),
                    "worth", format(result.worth()),
                    "currency", result.currency().name()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("wallet") || name.equals("frostwallet")) {
            if (args.length == 1) {
                return prefix(args[0], List.of("deposit", "withdraw", "reload", "reset"));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("withdraw")) {
                return currencyIds(args[1]);
            }
            return List.of();
        }
        if (name.equals("withdraw") || name.equals("with")) {
            if (args.length == 1) {
                return currencyIds(args[0]);
            }
            return List.of();
        }
        if (name.equals("depositall") && args.length == 1) {
            return currencyIds(args[0]);
        }
        return List.of();
    }

    private List<String> currencyIds(String prefix) {
        return prefix(prefix, config.currencies().stream().map(Currency::id).collect(Collectors.toList()));
    }

    private static List<String> prefix(String prefix, List<String> options) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private static Long parseAmount(String raw) {
        try {
            long value = Long.parseLong(raw);
            return value > 0L ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String format(long amount) {
        return String.format(Locale.US, "%,d", amount);
    }
}
