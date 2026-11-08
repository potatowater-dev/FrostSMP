package com.frost.plots.command;

import com.frost.plots.config.PluginConfig;
import com.frost.plots.gui.UpgradeGUI;
import com.frost.plots.hook.ItemCostHook;
import com.frost.plots.manager.MemberManager;
import com.frost.plots.manager.PlotManager;
import com.frost.plots.manager.PrivacyManager;
import com.frost.plots.model.Plot;
import com.frost.plots.model.UpgradeTier;
import com.frost.plots.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlotCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "home", "visit", "public", "private", "setspawn",
            "add", "remove", "promote", "upgrade", "info", "list", "delete"
    );

    private final PluginConfig config;
    private final PlotManager plots;
    private final PrivacyManager privacy;
    private final MemberManager members;
    private final UpgradeGUI upgradeGui;
    private final Map<UUID, Long> pendingDeletes = new ConcurrentHashMap<>();

    public PlotCommand(
            PluginConfig config,
            PlotManager plots,
            PrivacyManager privacy,
            MemberManager members,
            UpgradeGUI upgradeGui
    ) {
        this.config = config;
        this.plots = plots;
        this.privacy = privacy;
        this.members = members;
        this.upgradeGui = upgradeGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.prefixed("players-only"));
            return true;
        }
        if (!player.hasPermission("frostplots.use")) {
            MessageUtil.send(player, config.prefixed("no-permission"));
            return true;
        }
        if (args.length == 0) {
            MessageUtil.send(player, config.prefixed("usage"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "create" -> {
                if (!player.hasPermission("frostplots.create") && !player.hasPermission("frostplots.admin")) {
                    MessageUtil.send(player, config.prefixed("no-permission"));
                    yield true;
                }
                plots.create(player);
                yield true;
            }
            case "home" -> {
                plots.home(player);
                yield true;
            }
            case "visit" -> visit(player, args);
            case "public" -> {
                privacy.setPublic(player);
                yield true;
            }
            case "private" -> {
                privacy.setPrivate(player);
                yield true;
            }
            case "setspawn" -> {
                plots.setSpawn(player);
                yield true;
            }
            case "add" -> add(player, args);
            case "remove" -> remove(player, args);
            case "promote" -> promote(player, args);
            case "upgrade" -> {
                upgradeGui.open(player);
                yield true;
            }
            case "info" -> {
                plots.info(player);
                yield true;
            }
            case "list" -> list(player, args);
            case "delete" -> delete(player, args);
            case "setcost" -> setCost(player, args);
            default -> {
                MessageUtil.send(player, config.prefixed("usage"));
                yield true;
            }
        };
    }

    private boolean visit(Player player, String[] args) {
        if (args.length < 2) {
            plots.home(player);
            return true;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null) {
            MessageUtil.send(player, config.prefixed("player-not-found", "player", args[1]));
            return true;
        }
        plots.visit(player, target);
        return true;
    }

    private boolean add(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(player, config.prefixed("usage"));
            return true;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null) {
            MessageUtil.send(player, config.prefixed("player-not-found", "player", args[1]));
            return true;
        }
        members.add(player, target);
        return true;
    }

    private boolean remove(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(player, config.prefixed("usage"));
            return true;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null) {
            MessageUtil.send(player, config.prefixed("player-not-found", "player", args[1]));
            return true;
        }
        members.remove(player, target);
        return true;
    }

    private boolean promote(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(player, config.prefixed("usage-promote"));
            return true;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null) {
            MessageUtil.send(player, config.prefixed("player-not-found", "player", args[1]));
            return true;
        }
        String role = args.length >= 3 ? args[2] : "MEMBER";
        members.promote(player, target, role);
        return true;
    }

    private boolean list(Player player, String[] args) {
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }
        plots.list(player, page);
        return true;
    }

    private boolean delete(Player player, String[] args) {
        Plot plot = plots.requireOwnerForManagement(player);
        if (plot == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
            Long started = pendingDeletes.remove(player.getUniqueId());
            if (started == null || now - started > config.deleteConfirmSeconds() * 1000L) {
                MessageUtil.send(player, config.prefixed("delete-no-pending"));
                return true;
            }
            plots.delete(plot, player);
            return true;
        }
        pendingDeletes.put(player.getUniqueId(), now);
        MessageUtil.send(player, config.prefixed("delete-prompt", "seconds", String.valueOf(config.deleteConfirmSeconds())));
        return true;
    }

    private boolean setCost(Player player, String[] args) {
        if (!player.hasPermission("frostplots.admin")) {
            MessageUtil.send(player, config.prefixed("no-permission"));
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(player, config.prefixed("usage-setcost"));
            return true;
        }
        int tierNumber;
        try {
            tierNumber = Integer.parseInt(args[1]);
        } catch (NumberFormatException ignored) {
            MessageUtil.send(player, config.prefixed("invalid-tier"));
            return true;
        }
        int index = tierNumber - 1;
        if (index < 0 || index >= config.upgradeTiers().size()) {
            MessageUtil.send(player, config.prefixed("invalid-tier"));
            return true;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            MessageUtil.send(player, config.prefixed("empty-hand"));
            return true;
        }
        int amount = hand.getAmount();
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
                MessageUtil.send(player, config.prefixed("invalid-amount"));
                return true;
            }
        }
        if (amount < 0) {
            MessageUtil.send(player, config.prefixed("invalid-amount"));
            return true;
        }
        if (!config.saveUpgradeCost(index, amount, hand)) {
            MessageUtil.send(player, config.prefixed("invalid-tier"));
            return true;
        }
        UpgradeTier saved = config.tier(index);
        MessageUtil.send(player, config.prefixed(
                "cost-set",
                "tier", String.valueOf(tierNumber),
                "cost", saved.costAmount() + "x " + ItemCostHook.displayName(hand)
        ));
        return true;
    }

    private OfflinePlayer resolvePlayer(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(name)) {
                return online;
            }
        }
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(name)) {
                return offline;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission("frostplots.use")) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(SUBCOMMANDS);
            if (player.hasPermission("frostplots.admin")) {
                subs.add("setcost");
            }
            return filter(subs, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            return switch (sub) {
                case "visit", "add", "promote" -> filter(onlineNames(), args[1]);
                case "remove" -> filter(memberNames(player), args[1]);
                case "delete" -> filter(List.of("confirm"), args[1]);
                case "list" -> filter(List.of("1", "2"), args[1]);
                case "setcost" -> {
                    if (!player.hasPermission("frostplots.admin")) {
                        yield List.of();
                    }
                    List<String> tiers = new ArrayList<>();
                    for (int i = 1; i <= config.upgradeTiers().size(); i++) {
                        tiers.add(String.valueOf(i));
                    }
                    yield filter(tiers, args[1]);
                }
                default -> List.of();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("promote")) {
            return filter(List.of("VISITOR", "MEMBER", "CO_OWNER"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setcost") && player.hasPermission("frostplots.admin")) {
            return filter(List.of("1", "8", "16", "32", "64"), args[2]);
        }
        return List.of();
    }

    private List<String> memberNames(Player player) {
        Plot plot = plots.byOwner(player.getUniqueId());
        if (plot == null) {
            plot = plots.at(player.getLocation());
        }
        if (plot == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (UUID uuid : plot.getMembers().keySet()) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            if (offline.getName() != null) {
                names.add(offline.getName());
            }
        }
        return names;
    }

    private List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            names.add(online.getName());
        }
        return names;
    }

    private List<String> filter(List<String> options, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}
