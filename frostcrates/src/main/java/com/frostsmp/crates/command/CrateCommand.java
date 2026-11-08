package com.frostsmp.crates.command;

import com.frostsmp.crates.FrostCratesPlugin;
import com.frostsmp.crates.config.CrateDefinitionStorage;
import com.frostsmp.crates.config.MessagesConfig;
import com.frostsmp.crates.config.PlacedCrateStorage;
import com.frostsmp.crates.config.PluginConfig;
import com.frostsmp.crates.gui.CrateEditorGui;
import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.model.PlacedCrate;
import com.frostsmp.crates.service.CrateClaimService;
import com.frostsmp.crates.service.KeyService;
import com.frostsmp.crates.service.AutoKeyGrantService;
import com.frostsmp.crates.service.CrateCooldownService;
import com.frostsmp.crates.util.AdminPermissions;
import com.frostsmp.crates.util.BlockMaterialUtil;
import com.frostsmp.crates.util.MessageUtil;
import com.frostsmp.crates.util.PluginKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class CrateCommand implements CommandExecutor, TabCompleter {

    private final FrostCratesPlugin plugin;
    private final MessagesConfig messages;
    private final CrateDefinitionStorage definitions;
    private final PlacedCrateStorage placedCrates;
    private final KeyService keyService;
    private final CrateClaimService claimService;
    private final AutoKeyGrantService autoKeyGrantService;
    private final PluginKeys keys;

    public CrateCommand(
            FrostCratesPlugin plugin,
            MessagesConfig messages,
            CrateDefinitionStorage definitions,
            PlacedCrateStorage placedCrates,
            KeyService keyService,
            CrateClaimService claimService,
            AutoKeyGrantService autoKeyGrantService,
            PluginKeys keys
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.definitions = definitions;
        this.placedCrates = placedCrates;
        this.keyService = keyService;
        this.claimService = claimService;
        this.autoKeyGrantService = autoKeyGrantService;
        this.keys = keys;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> handleCreate(sender, args);
            case "wand" -> handleWand(sender, args);
            case "keys" -> handleKeys(sender, args);
            case "reload" -> handleReload(sender);
            case "respawn" -> handleRespawn(sender);
            case "list" -> handleList(sender);
            case "delete" -> handleDelete(sender, args);
            case "block" -> handleBlock(sender, args);
            case "claim" -> handleClaim(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            List<String> options = new ArrayList<>(List.of("claim", "keys"));
            if (AdminPermissions.isAdmin(sender)) {
                options.addAll(List.of("create", "wand", "block", "reload", "respawn", "list", "delete"));
            }
            return filter(args.length == 0 ? "" : args[0], options);
        }
        if (!AdminPermissions.isAdmin(sender)) {
            if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
                return filter(args[1], definitions.getIds());
            }
            return List.of();
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "create", "wand", "block", "claim", "delete" -> filter(args[1], definitions.getIds());
                case "keys" -> filter(args[1], "give", "take", "check");
                case "reload", "respawn", "list" -> List.of();
                default -> List.of();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("keys")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("keys")) {
            return filter(args[3], definitions.getIds());
        }
        return List.of();
    }

    private void handleClaim(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return;
        }
        if (!sender.hasPermission("frostsmp.crates.open")) {
            messages.send(sender, "no-permission-open");
            return;
        }
        if (args.length < 2) {
            messages.send(sender, "usage-claim");
            return;
        }
        String crateId = args[1].toLowerCase(Locale.ROOT);
        CrateDefinition definition = definitions.get(crateId);
        if (definition == null) {
            messages.send(sender, "unknown-crate", "crate", crateId);
            return;
        }
        claimService.claim(
                player,
                definition,
                CrateCooldownService.cooldownKey(player.getUniqueId(), crateId)
        );
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "<red>Players only.</red>");
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "<red>Usage: /crate create <name></red>");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z0-9_-]+")) {
            MessageUtil.send(sender, "<red>Crate name must be lowercase letters, numbers, underscores, or hyphens.</red>");
            return;
        }
        CrateDefinition definition = definitions.getOrCreate(name);
        player.openInventory(new CrateEditorGui(definition).getInventory());
        MessageUtil.send(player, "<green>Opened editor for crate <aqua>" + name + "</green>.");
    }

    private void handleWand(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "<red>Players only.</red>");
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "<red>Usage: /crate wand <name></red>");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        CrateDefinition definition = definitions.get(name);
        if (definition == null) {
            MessageUtil.send(sender, "<red>Unknown crate: " + name + "</red>");
            return;
        }
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gold>Crate Wand <gray>(" + name + ")"));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>Right-click block: place crate"),
                    MessageUtil.parse("<gray>Left-click crate: remove")
            ));
            meta.getPersistentDataContainer().set(keys.crateWand, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(keys.crateId, PersistentDataType.STRING, name);
            wand.setItemMeta(meta);
        }
        player.getInventory().addItem(wand);
        MessageUtil.send(player, "<green>Given crate wand for <aqua>" + name + "</green>.");
    }

    private void handleKeys(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String action = args[1].toLowerCase(Locale.ROOT);
            if (action.equals("give") || action.equals("take") || action.equals("check")) {
                if (!AdminPermissions.isAdmin(sender)) {
                    messages.send(sender, "no-permission-keys-admin");
                    return;
                }
                switch (action) {
                    case "give" -> keysGive(sender, args);
                    case "take" -> keysTake(sender, args);
                    case "check" -> keysCheck(sender, args);
                }
                return;
            }
        }
        if (sender instanceof Player player) {
            showOwnKeys(player);
            return;
        }
        MessageUtil.send(sender, "<red>Players only.</red>");
    }

    private void showOwnKeys(Player player) {
        Map<String, Integer> all = keyService.getAll(player);
        if (all.isEmpty()) {
            MessageUtil.send(player, "<gray>You have no crate keys.</gray>");
            return;
        }
        MessageUtil.send(player, "<aqua>Your crate keys:");
        all.forEach((crate, amount) -> player.sendMessage(Component.text(" - " + crate + ": " + amount)));
    }

    private void keysGive(CommandSender sender, String[] args) {
        if (args.length < 5) {
            MessageUtil.send(sender, "<red>Usage: /crate keys give <player> <crate> <amount></red>");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            MessageUtil.send(sender, "<red>Player not found.</red>");
            return;
        }
        String crate = args[3].toLowerCase(Locale.ROOT);
        if (definitions.get(crate) == null) {
            MessageUtil.send(sender, "<red>Unknown crate: " + crate + "</red>");
            return;
        }
        int amount = parsePositiveInt(args[4], sender);
        if (amount < 0) {
            return;
        }
        keyService.give(target, crate, amount);
        MessageUtil.send(sender, "<green>Gave <white>" + amount + "</white> key(s) for <aqua>" + crate + "</aqua> to <white>" + target.getName() + "</green>.");
    }

    private void keysTake(CommandSender sender, String[] args) {
        if (args.length < 5) {
            MessageUtil.send(sender, "<red>Usage: /crate keys take <player> <crate> <amount></red>");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            MessageUtil.send(sender, "<red>Player not found.</red>");
            return;
        }
        String crate = args[3].toLowerCase(Locale.ROOT);
        int amount = parsePositiveInt(args[4], sender);
        if (amount < 0) {
            return;
        }
        keyService.take(target, crate, amount);
        MessageUtil.send(sender, "<yellow>Took <white>" + amount + "</white> key(s) for <aqua>" + crate + "</aqua> from <white>" + target.getName() + "</yellow>.");
    }

    private void keysCheck(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "<red>Usage: /crate keys check <player> [crate]</red>");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            MessageUtil.send(sender, "<red>Player not found.</red>");
            return;
        }
        if (args.length >= 4) {
            String crate = args[3].toLowerCase(Locale.ROOT);
            int balance = keyService.get(target, crate);
            MessageUtil.send(sender, "<aqua>" + target.getName() + "</aqua> has <white>" + balance + "</white> key(s) for <aqua>" + crate + "</aqua>.");
            return;
        }
        Map<String, Integer> all = keyService.getAll(target);
        if (all.isEmpty()) {
            MessageUtil.send(sender, "<gray>" + target.getName() + " has no keys.</gray>");
            return;
        }
        MessageUtil.send(sender, "<aqua>Keys for " + target.getName() + ":");
        all.forEach((crate, amount) -> sender.sendMessage(Component.text(" - " + crate + ": " + amount)));
    }

    private void handleReload(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        plugin.safeReload();
        messages.send(sender, "reload-success");
    }

    private void handleRespawn(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        plugin.getHologramService().respawnAll();
        messages.send(sender, "respawn-success");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            messages.send(sender, "usage-delete");
            return;
        }
        String crateId = args[1].toLowerCase(Locale.ROOT);
        if (definitions.get(crateId) == null) {
            messages.send(sender, "unknown-crate", "crate", crateId);
            return;
        }
        java.util.List<PlacedCrate> removed = placedCrates.unregisterAllForDefinition(crateId);
        for (PlacedCrate placed : removed) {
            plugin.getHologramService().remove(placed);
            var block = placed.getLocation().getBlock();
            if (plugin.getCrateService().isDisplayBlockMaterial(block.getType())) {
                block.setType(org.bukkit.Material.AIR, false);
            }
        }
        definitions.delete(crateId);
        autoKeyGrantService.removeCrate(crateId);
        messages.send(sender, "delete-success", "crate", crateId, "count", String.valueOf(removed.size()));
    }

    private void handleBlock(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return;
        }
        if (args.length < 2) {
            messages.send(sender, "usage-block");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        CrateDefinition definition = definitions.get(name);
        if (definition == null) {
            messages.send(sender, "unknown-crate", "crate", name);
            return;
        }
        Material blockMaterial = BlockMaterialUtil.normalizeDisplayBlock(definition.getDisplayBlock());
        ItemStack crateBlock = new ItemStack(blockMaterial);
        ItemMeta meta = crateBlock.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<aqua>" + definition.getDisplayName() + " <gray>Crate Block"));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>Place to register this crate"),
                    MessageUtil.parse("<gray>ID: <white>" + name)
            ));
            meta.getPersistentDataContainer().set(keys.crateBlock, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(keys.crateId, PersistentDataType.STRING, name);
            crateBlock.setItemMeta(meta);
        }
        player.getInventory().addItem(crateBlock);
        messages.send(player, "block-given", "crate", name);
    }

    private void handleList(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (definitions.getIds().isEmpty()) {
            MessageUtil.send(sender, "<gray>No crate definitions.</gray>");
            return;
        }
        MessageUtil.send(sender, "<aqua>Crate definitions:");
        for (String id : definitions.getIds()) {
            int placed = placedCrates.countForDefinition(id);
            MessageUtil.send(sender, "<white>- " + id + " <gray>(" + placed + " placed)</gray>");
        }
    }

    private void sendHelp(CommandSender sender) {
        messages.send(sender, "help-header");
        messages.send(sender, "help-claim");
        messages.send(sender, "help-keys");
        if (AdminPermissions.isAdmin(sender)) {
            messages.send(sender, "help-create");
            messages.send(sender, "help-wand");
            messages.send(sender, "help-block");
            messages.send(sender, "help-delete");
            messages.send(sender, "help-reload");
            messages.send(sender, "help-respawn");
            messages.send(sender, "help-list");
        }
    }

    private boolean requireAdmin(CommandSender sender) {
        if (AdminPermissions.isAdmin(sender)) {
            return true;
        }
        messages.send(sender, "no-permission-admin");
        return false;
    }

    private int parsePositiveInt(String input, CommandSender sender) {
        try {
            int value = Integer.parseInt(input);
            if (value < 0) {
                MessageUtil.send(sender, "<red>Amount must be non-negative.</red>");
                return -1;
            }
            return value;
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "<red>Invalid number.</red>");
            return -1;
        }
    }

    private List<String> filter(String input, String... options) {
        return filter(input, Arrays.asList(options));
    }

    private List<String> filter(String input, List<String> options) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
