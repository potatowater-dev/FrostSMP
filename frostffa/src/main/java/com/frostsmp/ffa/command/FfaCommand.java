package com.frostsmp.ffa.command;

import com.frostsmp.ffa.config.PluginConfig;
import com.frostsmp.ffa.kit.KitService;
import com.frostsmp.ffa.mutator.MutatorId;
import com.frostsmp.ffa.mutator.MutatorManager;
import com.frostsmp.ffa.schedule.ScheduleService;
import com.frostsmp.ffa.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FfaCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN = "frostsmp.ffa.admin";
    private static final String KIT = "frostsmp.ffa.kit";

    private final PluginConfig config;
    private final KitService kits;
    private final MutatorManager mutators;
    private final ScheduleService schedule;
    private final Runnable reload;

    public FfaCommand(PluginConfig config, KitService kits, MutatorManager mutators,
                      ScheduleService schedule, Runnable reload) {
        this.config = config;
        this.kits = kits;
        this.mutators = mutators;
        this.schedule = schedule;
        this.reload = reload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            MessageUtil.send(sender, config.prefixed("usage"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "kit" -> {
                if (!(sender instanceof Player player)) {
                    MessageUtil.send(sender, config.prefixed("players-only"));
                    return true;
                }
                if (!player.hasPermission(KIT)) {
                    MessageUtil.send(sender, config.prefixed("no-permission"));
                    return true;
                }
                String kit = kits.give(player);
                MessageUtil.send(player, config.prefixed("kit-given", "kit", kit));
            }
            case "mutator" -> handleMutator(sender, args);
            case "schedule" -> {
                if (!sender.hasPermission(ADMIN)) {
                    MessageUtil.send(sender, config.prefixed("no-permission"));
                    return true;
                }
                for (ScheduleService.Slot slot : schedule.slots()) {
                    MessageUtil.send(sender, config.prefixed("schedule-line",
                            "time", slot.time().toString(),
                            "type", slot.type()));
                }
            }
            case "reload" -> {
                if (!sender.hasPermission(ADMIN)) {
                    MessageUtil.send(sender, config.prefixed("no-permission"));
                    return true;
                }
                reload.run();
                MessageUtil.send(sender, config.prefixed("reloaded"));
            }
            default -> MessageUtil.send(sender, config.prefixed("usage"));
        }
        return true;
    }

    private void handleMutator(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            MessageUtil.send(sender, config.prefixed("no-permission"));
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, config.prefixed("usage"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                String list = Stream.of(MutatorId.values()).map(MutatorId::id).collect(Collectors.joining(", "));
                MessageUtil.send(sender, config.prefixed("mutator-list", "list", list));
            }
            case "stop" -> {
                if (!mutators.isActive()) {
                    MessageUtil.send(sender, config.prefixed("mutator-none"));
                    return;
                }
                mutators.stop();
            }
            case "start" -> {
                if (mutators.isActive()) {
                    MessageUtil.send(sender, config.prefixed("mutator-already"));
                    return;
                }
                if (args.length < 3 || args[2].equalsIgnoreCase("random")) {
                    mutators.startRandom();
                    return;
                }
                MutatorId id = MutatorId.fromId(args[2]);
                if (id == null) {
                    MessageUtil.send(sender, config.prefixed("mutator-unknown", "id", args[2]));
                    return;
                }
                mutators.start(id);
            }
            default -> MessageUtil.send(sender, config.prefixed("usage"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return prefix(args[0], List.of("kit", "mutator", "schedule", "reload"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("mutator") && sender.hasPermission(ADMIN)) {
            return prefix(args[1], List.of("list", "start", "stop"));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("mutator") && args[1].equalsIgnoreCase("start")) {
            List<String> ids = new ArrayList<>();
            ids.add("random");
            for (MutatorId id : MutatorId.values()) {
                ids.add(id.id());
            }
            return prefix(args[2], ids);
        }
        return List.of();
    }

    private static List<String> prefix(String raw, List<String> options) {
        String prefix = raw.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix)) {
                result.add(option);
            }
        }
        return result;
    }
}
