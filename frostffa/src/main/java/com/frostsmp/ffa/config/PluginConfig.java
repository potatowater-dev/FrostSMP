package com.frostsmp.ffa.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PluginConfig {

    private final JavaPlugin plugin;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public String arenaWorld() {
        return plugin.getConfig().getString("arena.world", "world");
    }

    public String arenaRegion() {
        return plugin.getConfig().getString("arena.region", "ffa");
    }

    public boolean clearInventory() {
        return plugin.getConfig().getBoolean("kits.clear-inventory", true);
    }

    public boolean giveOnJoin() {
        return plugin.getConfig().getBoolean("kits.give-on-join", true);
    }

    public boolean giveOnRespawn() {
        return plugin.getConfig().getBoolean("kits.give-on-respawn", true);
    }

    public int durationMinMinutes() {
        return Math.max(1, plugin.getConfig().getInt("mutators.duration-minutes.min", 30));
    }

    public int durationMaxMinutes() {
        return Math.max(durationMinMinutes(), plugin.getConfig().getInt("mutators.duration-minutes.max", 40));
    }

    public Map<String, List<String>> blockedAfter() {
        Map<String, List<String>> map = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mutators.blocked-after");
        if (section == null) {
            return map;
        }
        for (String key : section.getKeys(false)) {
            map.put(key, new ArrayList<>(section.getStringList(key)));
        }
        return map;
    }

    public int shrinkingFinalSize() {
        return Math.max(2, plugin.getConfig().getInt("mutators.shrinking-border.final-size-blocks", 48));
    }

    public int shrinkingMinutes() {
        return Math.max(1, plugin.getConfig().getInt("mutators.shrinking-border.shrink-minutes", 15));
    }

    public double shrinkingDamage() {
        return Math.max(0.0, plugin.getConfig().getDouble("mutators.shrinking-border.damage-per-second", 0.2));
    }

    public double shrinkingDamageBuffer() {
        return Math.max(0.0, plugin.getConfig().getDouble("mutators.shrinking-border.damage-buffer-blocks", 5.0));
    }

    public int shrinkingWarningDistance() {
        return Math.max(0, plugin.getConfig().getInt("mutators.shrinking-border.warning-distance", 5));
    }

    public int fogStageMinutes() {
        return Math.max(1, plugin.getConfig().getInt("mutators.fog-of-war.stage-minutes", 10));
    }

    public String fogStageTitle(int stage) {
        return plugin.getConfig().getString("mutators.fog-of-war.stage-titles." + stage + ".title",
                "<dark_gray>FOG OF WAR</dark_gray>");
    }

    public String fogStageSubtitle(int stage) {
        return plugin.getConfig().getString("mutators.fog-of-war.stage-titles." + stage + ".subtitle",
                "<white>Darkness " + toRoman(stage) + "</white>");
    }

    public int titleFadeInTicks() {
        return Math.max(0, plugin.getConfig().getInt("titles.fade-in-ticks", 10));
    }

    public int titleStayTicks() {
        return Math.max(1, plugin.getConfig().getInt("titles.stay-ticks", 50));
    }

    public int titleFadeOutTicks() {
        return Math.max(0, plugin.getConfig().getInt("titles.fade-out-ticks", 15));
    }

    public String mutatorTitle(String id) {
        return plugin.getConfig().getString("titles." + id + ".title", "<gold>{name}</gold>");
    }

    public String mutatorSubtitle(String id) {
        return plugin.getConfig().getString("titles." + id + ".subtitle", "");
    }

    public int strengthAmplifier() {
        return Math.max(0, plugin.getConfig().getInt("mutators.strength-iii.amplifier", 2));
    }

    public int randomEffectIntervalSeconds() {
        return Math.max(10, plugin.getConfig().getInt("mutators.random-effects.interval-seconds", 600));
    }

    public int randomEffectDurationSeconds() {
        return Math.max(10, plugin.getConfig().getInt("mutators.random-effects.duration-seconds", 600));
    }

    public String blackoutHiddenName() {
        return plugin.getConfig().getString("mutators.blackout.hidden-name", " ");
    }

    public String blackoutSkinPlayer() {
        return plugin.getConfig().getString("mutators.blackout.skin-player", "OperatorExit9");
    }

    public int maceRouletteDoubleJumpCooldownSeconds() {
        return Math.max(1, plugin.getConfig().getInt("mutators.mace-roulette.double-jump-cooldown-seconds", 5));
    }

    public double maceRouletteDoubleJumpStrength() {
        return plugin.getConfig().getDouble("mutators.mace-roulette.double-jump-strength", 0.72D);
    }

    public int maceRouletteAfkSeconds() {
        return Math.max(0, plugin.getConfig().getInt("mutators.mace-roulette.afk-seconds", 10));
    }

    public int maceRouletteKillTokens() {
        return Math.max(0, plugin.getConfig().getInt("mutators.mace-roulette.kill-tokens", 3));
    }

    public String timezone() {
        return plugin.getConfig().getString("schedule.timezone", "UTC");
    }

    public List<Map<String, Object>> scheduleSlots() {
        List<Map<String, Object>> slots = new ArrayList<>();
        List<Map<?, ?>> raw = plugin.getConfig().getMapList("schedule.slots");
        for (Map<?, ?> entry : raw) {
            Map<String, Object> copy = new HashMap<>();
            for (Map.Entry<?, ?> e : entry.entrySet()) {
                copy.put(String.valueOf(e.getKey()), e.getValue());
            }
            slots.add(copy);
        }
        return slots;
    }

    public String msg(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    public String prefixed(String key, String... kv) {
        return plugin.getConfig().getString("messages.prefix", "") + MessageFormat.format(msg(key), kv);
    }

    private static final class MessageFormat {
        private MessageFormat() {
        }

        static String format(String template, String... kv) {
            return com.frostsmp.ffa.util.MessageUtil.format(template, kv);
        }
    }

    private static String toRoman(int stage) {
        return switch (stage) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(stage);
        };
    }
}
