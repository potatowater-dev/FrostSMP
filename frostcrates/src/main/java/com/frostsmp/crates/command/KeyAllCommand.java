package com.frostsmp.crates.command;

import com.frostsmp.crates.FrostCratesPlugin;
import com.frostsmp.crates.config.CrateDefinitionStorage;
import com.frostsmp.crates.config.MessagesConfig;
import com.frostsmp.crates.service.KeyService;
import com.frostsmp.crates.util.AdminPermissions;
import com.frostsmp.crates.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class KeyAllCommand implements CommandExecutor, TabCompleter {

    private final FrostCratesPlugin plugin;
    private final MessagesConfig messages;
    private final CrateDefinitionStorage definitions;
    private final KeyService keyService;

    public KeyAllCommand(
            FrostCratesPlugin plugin,
            MessagesConfig messages,
            CrateDefinitionStorage definitions,
            KeyService keyService
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.definitions = definitions;
        this.keyService = keyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!AdminPermissions.isAdmin(sender)) {
            messages.send(sender, "no-permission-keys-admin");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage-keyall");
            return true;
        }

        String crate = args[0].toLowerCase(Locale.ROOT);
        if (definitions.get(crate) == null) {
            messages.send(sender, "unknown-crate", "crate", crate);
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            MessageUtil.send(sender, "<red>Amount must be a number.</red>");
            return true;
        }
        if (amount < 1) {
            MessageUtil.send(sender, "<red>Amount must be at least 1.</red>");
            return true;
        }

        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (online.isEmpty()) {
            messages.send(sender, "keyall-none-online");
            return true;
        }

        for (Player player : online) {
            keyService.give(player, crate, amount);
            plugin.getHologramService().refreshKeysFor(player);
            messages.send(player, "keyall-received", "amount", String.valueOf(amount), "crate", crate);
        }
        keyService.flush();

        messages.send(sender, "keyall-success",
                "amount", String.valueOf(amount),
                "crate", crate,
                "count", String.valueOf(online.size()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!AdminPermissions.isAdmin(sender)) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(args[0], definitions.getIds());
        }
        if (args.length == 2) {
            return filter(args[1], List.of("1", "5", "10"));
        }
        return List.of();
    }

    private static List<String> filter(String input, Collection<String> options) {
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(option);
            }
        }
        return out;
    }
}
