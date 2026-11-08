package com.frostsmp.grapple.config;

import com.frostsmp.grapple.util.MessageUtil;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

public final class GrappleConfig {

    private final JavaPlugin plugin;
    private double strength;
    private double speed;
    private double range;
    private int cooldownSeconds;
    private String cooldownDisplay;
    private double verticalBoost;
    private boolean fallDamage;
    private int fallProtectionTicks;
    private List<String> enabledWorlds;
    private String itemName;
    private List<String> itemLore;
    private boolean unbreakable;

    public GrappleConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        strength = Math.max(0.1, plugin.getConfig().getDouble("strength", 1.25));
        speed = Math.max(0.1, plugin.getConfig().getDouble("speed", 1.5));
        range = Math.max(1.0, plugin.getConfig().getDouble("range", 48.0));
        cooldownSeconds = Math.max(0, plugin.getConfig().getInt("cooldown-seconds", 2));
        cooldownDisplay = plugin.getConfig().getString("cooldown-display", "both");
        if (cooldownDisplay == null || cooldownDisplay.isBlank()) {
            cooldownDisplay = "both";
        } else {
            cooldownDisplay = cooldownDisplay.trim().toLowerCase(Locale.ROOT);
        }
        verticalBoost = plugin.getConfig().getDouble("vertical-boost", 0.3);
        fallDamage = plugin.getConfig().getBoolean("fall-damage", false);
        fallProtectionTicks = Math.max(1, plugin.getConfig().getInt("fall-protection-ticks", 100));
        enabledWorlds = plugin.getConfig().getStringList("enabled-worlds").stream()
                .map(world -> world.toLowerCase(Locale.ROOT))
                .toList();
        itemName = plugin.getConfig().getString("item.name", "<gold><bold>Grappling Hook</bold></gold>");
        itemLore = plugin.getConfig().getStringList("item.lore");
        unbreakable = plugin.getConfig().getBoolean("item.unbreakable", true);
    }

    public String prefixed(String key, String... kv) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String body = plugin.getConfig().getString("messages." + key, "");
        return prefix + MessageUtil.format(body, kv);
    }

    public boolean isWorldEnabled(World world) {
        if (enabledWorlds == null || enabledWorlds.isEmpty()) {
            return true;
        }
        return enabledWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }

    public double strength() {
        return strength;
    }

    public double speed() {
        return speed;
    }

    public double range() {
        return range;
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public boolean showCooldownActionBar() {
        return "actionbar".equals(cooldownDisplay) || "both".equals(cooldownDisplay);
    }

    public boolean showCooldownBossBar() {
        return "bossbar".equals(cooldownDisplay) || "both".equals(cooldownDisplay);
    }

    public double verticalBoost() {
        return verticalBoost;
    }

    public boolean fallDamage() {
        return fallDamage;
    }

    public int fallProtectionTicks() {
        return fallProtectionTicks;
    }

    public String itemName() {
        return itemName;
    }

    public List<String> itemLore() {
        return itemLore;
    }

    public boolean unbreakable() {
        return unbreakable;
    }
}
