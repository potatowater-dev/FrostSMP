package com.frostsmp.alt.command;

import com.frostsmp.alt.FrostAltPlugin;
import com.frostsmp.alt.model.AltCase;
import com.frostsmp.alt.service.EnforcementService;
import com.frostsmp.alt.storage.CaseStorage;
import com.frostsmp.alt.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FrostAltCommand implements CommandExecutor, TabCompleter {

    private final FrostAltPlugin plugin;
    private final CaseStorage caseStorage;
    private final EnforcementService enforcementService;

    public FrostAltCommand(FrostAltPlugin plugin, CaseStorage caseStorage, EnforcementService enforcementService) {
        this.plugin = plugin;
        this.caseStorage = caseStorage;
        this.enforcementService = enforcementService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("frostsmp.alt.admin")) {
            MessageUtil.send(sender, "<red>You do not have permission.</red>");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadPluginConfig();
                MessageUtil.send(sender, "<green>FrostAlt config reloaded.</green>");
            }
            case "cases" -> listCases(sender);
            case "case" -> {
                if (args.length < 2) {
                    MessageUtil.send(sender, "<yellow>Usage: /frostalt case <id></yellow>");
                    return true;
                }
                showCase(sender, args[1]);
            }
            case "approve" -> {
                if (args.length < 2) {
                    MessageUtil.send(sender, "<yellow>Usage: /frostalt approve <player|caseId></yellow>");
                    return true;
                }
                approve(sender, args[1]);
            }
            case "deny" -> {
                if (args.length < 2) {
                    MessageUtil.send(sender, "<yellow>Usage: /frostalt deny <player|caseId></yellow>");
                    return true;
                }
                deny(sender, args[1]);
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void listCases(CommandSender sender) {
        List<AltCase> openCases = caseStorage.getOpenCases();
        if (openCases.isEmpty()) {
            MessageUtil.send(sender, "<gray>No open cases.</gray>");
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (AltCase altCase : openCases) {
            String appeal = altCase.getAppealText() == null ? "none" : altCase.getAppealText();
            MessageUtil.send(sender, "<yellow>#" + altCase.getId() + "</yellow> "
                    + altCase.getFlaggedPlayerName() + " <gray>| IP " + altCase.getIp()
                    + " | " + format.format(new Date(altCase.getCreatedAt()))
                    + " | appeal: " + appeal + "</gray>");
        }
    }

    private void showCase(CommandSender sender, String idArg) {
        AltCase altCase = resolveCase(idArg);
        if (altCase == null) {
            MessageUtil.send(sender, "<red>Case not found.</red>");
            return;
        }
        MessageUtil.send(sender, "<gold>Case #" + altCase.getId() + "</gold>");
        MessageUtil.send(sender, "<gray>IP:</gray> " + altCase.getIp());
        MessageUtil.send(sender, "<gray>Flagged:</gray> " + altCase.getFlaggedPlayerName());
        MessageUtil.send(sender, "<gray>Status:</gray> " + altCase.getStatus().name());
        if (!altCase.getLinkedAccountIds().isEmpty()) {
            MessageUtil.send(sender, "<gray>Linked accounts:</gray> " + altCase.getLinkedAccountIds().size());
        }
        for (Map.Entry<String, Integer> entry : altCase.getPlaytimeMinutes().entrySet()) {
            MessageUtil.send(sender, "<gray>Playtime " + entry.getKey() + ":</gray> " + entry.getValue() + "m");
        }
        if (altCase.getAppealText() != null) {
            MessageUtil.send(sender, "<gray>Appeal:</gray> " + altCase.getAppealText());
        }
    }

    private void approve(CommandSender sender, String target) {
        AltCase altCase = resolveCase(target);
        if (altCase == null || !altCase.isOpen()) {
            MessageUtil.send(sender, "<red>Open case not found.</red>");
            return;
        }
        enforcementService.approveCase(altCase);
        MessageUtil.send(sender, "<green>Approved case #" + altCase.getId() + " for " + altCase.getFlaggedPlayerName() + ".</green>");
    }

    private void deny(CommandSender sender, String target) {
        AltCase altCase = resolveCase(target);
        if (altCase == null || !altCase.isOpen()) {
            MessageUtil.send(sender, "<red>Open case not found.</red>");
            return;
        }
        enforcementService.denyCase(altCase, sender.getName());
        MessageUtil.send(sender, "<red>Denied case #" + altCase.getId() + " for " + altCase.getFlaggedPlayerName() + ".</red>");
    }

    private AltCase resolveCase(String target) {
        try {
            int id = Integer.parseInt(target);
            AltCase byId = caseStorage.getCase(id);
            if (byId != null) {
                return byId;
            }
        } catch (NumberFormatException ignored) {
        }
        Player online = Bukkit.getPlayerExact(target);
        if (online != null) {
            return caseStorage.findOpenCaseForPlayer(online.getUniqueId());
        }
        for (AltCase altCase : caseStorage.getOpenCases()) {
            if (altCase.getFlaggedPlayerName().equalsIgnoreCase(target)) {
                return altCase;
            }
        }
        return null;
    }

    private void sendUsage(CommandSender sender) {
        MessageUtil.send(sender, "<yellow>Usage: /frostalt <cases|case|approve|deny|reload></yellow>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("frostsmp.alt.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            for (String option : List.of("cases", "case", "approve", "deny", "reload")) {
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    options.add(option);
                }
            }
            return options;
        }
        return List.of();
    }
}
