package com.frostsmp.killeffect.command;

import com.frostsmp.killeffect.FrostKillEffectsPlugin;
import com.frostsmp.killeffect.config.KillEffectConfig;
import com.frostsmp.killeffect.gui.SelectGui;
import com.frostsmp.killeffect.model.KillEffectDefinition;
import com.frostsmp.killeffect.storage.PlayerEffectStorage;
import com.frostsmp.killeffect.util.MessageUtil;
import org.bukkit.Bukkit;
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
import java.util.Set;

public final class KillEffectCommand implements CommandExecutor, TabCompleter {

    private final FrostKillEffectsPlugin plugin;
    private final KillEffectConfig config;
    private final PlayerEffectStorage storage;

    public KillEffectCommand(FrostKillEffectsPlugin plugin, KillEffectConfig config, PlayerEffectStorage storage) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                return openSelect(player);
            }
            MessageUtil.send(sender, config.prefixed("usage"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "reload" -> reload(sender);
            case "give" -> give(sender, args);
            case "setitem" -> setItem(sender, args);
            case "select" -> select(sender, args);
            case "list" -> {
                if (!(sender instanceof Player player)) {
                    MessageUtil.send(sender, config.prefixed("players-only"));
                    yield true;
                }
                yield list(player);
            }
            case "clear" -> clear(sender);
            default -> {
                MessageUtil.send(sender, config.prefixed("usage"));
                yield true;
            }
        };
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("frostsmp.killeffect.admin")) {
            MessageUtil.send(sender, config.prefixed("no-permission"));
            return true;
        }
        plugin.reloadAll();
        MessageUtil.send(sender, config.prefixed("reloaded"));
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostsmp.killeffect.admin")) {
            MessageUtil.send(sender, config.prefixed("no-permission"));
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, config.prefixed("usage"));
            return true;
        }
        KillEffectDefinition effect = config.effect(args[1]);
        if (effect == null) {
            MessageUtil.send(sender, config.prefixed("unknown-effect", "effect", args[1]));
            return true;
        }
        Player target;
        int amount = 1;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                MessageUtil.send(sender, config.prefixed("player-offline"));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            MessageUtil.send(sender, config.prefixed("players-only"));
            return true;
        }
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException exception) {
                MessageUtil.send(sender, config.prefixed("usage"));
                return true;
            }
        }
        ItemStack stack = effect.createClaimStack(amount);
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(stack);
        leftover.values().forEach(drop -> target.getWorld().dropItemNaturally(target.getLocation(), drop));
        MessageUtil.send(sender, config.prefixed("given",
                "amount", String.valueOf(amount),
                "effect", effect.name(),
                "player", target.getName()));
        return true;
    }

    private boolean setItem(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostsmp.killeffect.admin")) {
            MessageUtil.send(sender, config.prefixed("no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.prefixed("players-only"));
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, config.prefixed("usage"));
            return true;
        }
        KillEffectDefinition effect = config.effect(args[1]);
        if (effect == null) {
            MessageUtil.send(sender, config.prefixed("unknown-effect", "effect", args[1]));
            return true;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            MessageUtil.send(sender, config.prefixed("empty-hand"));
            return true;
        }
        config.saveClaimItem(effect.id(), hand);
        MessageUtil.send(sender, config.prefixed("setitem", "effect", effect.name()));
        return true;
    }

    private boolean select(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.prefixed("players-only"));
            return true;
        }
        if (!player.hasPermission("frostsmp.killeffect.use")) {
            MessageUtil.send(player, config.prefixed("no-permission"));
            return true;
        }
        if (args.length < 2) {
            return openSelect(player);
        }
        if (args[1].equalsIgnoreCase("none") || args[1].equalsIgnoreCase("off") || args[1].equalsIgnoreCase("clear")) {
            storage.select(player.getUniqueId(), null);
            MessageUtil.send(player, config.prefixed("cleared"));
            return true;
        }
        KillEffectDefinition effect = config.effect(args[1]);
        if (effect == null) {
            MessageUtil.send(player, config.prefixed("unknown-effect", "effect", args[1]));
            return true;
        }
        if (!storage.hasClaimed(player.getUniqueId(), effect.id())) {
            MessageUtil.send(player, config.prefixed("not-owned", "effect", effect.name()));
            return true;
        }
        storage.select(player.getUniqueId(), effect.id());
        MessageUtil.send(player, config.prefixed("selected", "effect", effect.name()));
        return true;
    }

    private boolean openSelect(Player player) {
        if (!player.hasPermission("frostsmp.killeffect.use")) {
            MessageUtil.send(player, config.prefixed("no-permission"));
            return true;
        }
        SelectGui.open(player, config, storage);
        return true;
    }

    private boolean clear(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.prefixed("players-only"));
            return true;
        }
        storage.select(player.getUniqueId(), null);
        MessageUtil.send(player, config.prefixed("cleared"));
        return true;
    }

    private boolean list(Player player) {
        if (!player.hasPermission("frostsmp.killeffect.use")) {
            MessageUtil.send(player, config.prefixed("no-permission"));
            return true;
        }
        Set<String> owned = storage.claimed(player.getUniqueId());
        if (owned.isEmpty()) {
            MessageUtil.send(player, config.prefixed("list-empty"));
            return true;
        }
        MessageUtil.send(player, config.prefixed("list-header"));
        String selected = storage.selected(player.getUniqueId());
        for (String id : owned) {
            KillEffectDefinition effect = config.effect(id);
            String name = effect == null ? id : effect.name();
            String mark = id.equalsIgnoreCase(selected) ? config.msg("list-selected") : "";
            MessageUtil.send(player, config.prefixed("list-line", "effect", name, "selected", mark));
        }
        if (selected == null || selected.isBlank()) {
            MessageUtil.send(player, config.prefixed("none-selected"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("list");
            options.add("select");
            options.add("clear");
            if (sender.hasPermission("frostsmp.killeffect.admin")) {
                options.add("give");
                options.add("setitem");
                options.add("reload");
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return options.stream().filter(option -> option.startsWith(prefix)).toList();
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("give") || sub.equals("setitem")) {
                if (!sender.hasPermission("frostsmp.killeffect.admin")) {
                    return List.of();
                }
                return config.effectIds().stream().filter(id -> id.startsWith(prefix)).toList();
            }
            if (sub.equals("select") && sender instanceof Player player) {
                List<String> options = new ArrayList<>(storage.claimed(player.getUniqueId()));
                options.add("none");
                return options.stream().filter(id -> id.startsWith(prefix)).toList();
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("frostsmp.killeffect.admin")) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
