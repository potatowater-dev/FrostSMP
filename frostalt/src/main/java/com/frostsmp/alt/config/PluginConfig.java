package com.frostsmp.alt.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PluginConfig {

    private final JavaPlugin plugin;
    private boolean sameIpConcurrent;
    private String appealWorldName;
    private double appealX;
    private double appealY;
    private double appealZ;
    private float appealYaw;
    private float appealPitch;
    private Set<String> allowedAppealCommands;
    private String kickMessage;
    private String mainNotifyMessage;
    private String appealPromptMessage;
    private String appealSubmittedMessage;
    private String approvedMessage;
    private String deniedMessage;
    private String commandBlockedMessage;
    private String appealTitle;
    private String appealSubtitle;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        sameIpConcurrent = config.getBoolean("detection.same-ip-concurrent", true);
        appealWorldName = config.getString("appeal-world", "world");
        appealX = config.getDouble("appeal-spawn.x", 0.5);
        appealY = config.getDouble("appeal-spawn.y", 200.0);
        appealZ = config.getDouble("appeal-spawn.z", 0.5);
        appealYaw = (float) config.getDouble("appeal-spawn.yaw", 0.0);
        appealPitch = (float) config.getDouble("appeal-spawn.pitch", 0.0);

        allowedAppealCommands = new HashSet<>();
        for (String command : config.getStringList("allowed-appeal-commands")) {
            allowedAppealCommands.add(command.toLowerCase(Locale.ROOT));
        }
        if (allowedAppealCommands.isEmpty()) {
            allowedAppealCommands.add("appeal");
        }

        kickMessage = config.getString("messages.kick",
                "<red>Alt account detected on your IP (lowest playtime). Rejoin to submit an appeal.</red>");
        mainNotifyMessage = config.getString("messages.main-notify",
                "<yellow>An account on your IP was flagged as a possible alt. Your account is unaffected.</yellow>");
        appealPromptMessage = config.getString("messages.appeal-prompt",
                "<yellow>Use /appeal <reason> if this was a mistake (e.g. sibling).</yellow>");
        appealSubmittedMessage = config.getString("messages.appeal-submitted",
                "<green>Appeal submitted. Staff will review your case.</green>");
        approvedMessage = config.getString("messages.approved",
                "<green>Your appeal was approved. Welcome back.</green>");
        deniedMessage = config.getString("messages.denied",
                "<red>Your appeal was denied. You are permanently banned.</red>");
        commandBlockedMessage = config.getString("messages.command-blocked",
                "<red>You can only use /appeal while your case is under review.</red>");
        appealTitle = config.getString("messages.appeal-title", "<red>Alt Detected</red>");
        appealSubtitle = config.getString("messages.appeal-subtitle", "<yellow>Submit /appeal <reason></yellow>");
    }

    public boolean isSameIpConcurrent() {
        return sameIpConcurrent;
    }

    public Location getAppealSpawn() {
        World world = Bukkit.getWorld(appealWorldName);
        if (world == null) {
            world = Bukkit.getWorlds().getFirst();
        }
        return new Location(world, appealX, appealY, appealZ, appealYaw, appealPitch);
    }

    public boolean isAppealCommandAllowed(String label) {
        return allowedAppealCommands.contains(label.toLowerCase(Locale.ROOT));
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public String getMainNotifyMessage() {
        return mainNotifyMessage;
    }

    public String getAppealPromptMessage() {
        return appealPromptMessage;
    }

    public String getAppealSubmittedMessage() {
        return appealSubmittedMessage;
    }

    public String getApprovedMessage() {
        return approvedMessage;
    }

    public String getDeniedMessage() {
        return deniedMessage;
    }

    public String getCommandBlockedMessage() {
        return commandBlockedMessage;
    }

    public String getAppealTitle() {
        return appealTitle;
    }

    public String getAppealSubtitle() {
        return appealSubtitle;
    }
}
