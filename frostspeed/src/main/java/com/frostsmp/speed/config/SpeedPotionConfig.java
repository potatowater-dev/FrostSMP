package com.frostsmp.speed.config;

import org.bukkit.attribute.AttributeModifier;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpeedPotionConfig {

    private final JavaPlugin plugin;
    private double magnitude;
    private AttributeModifier.Operation operation;
    private int durationTicks;
    private double particleRadius;
    private int amplifier;

    public SpeedPotionConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        magnitude = plugin.getConfig().getDouble("magnitude", 0.20);
        durationTicks = Math.max(1, plugin.getConfig().getInt("duration-ticks", 300));
        particleRadius = Math.max(0.0, plugin.getConfig().getDouble("particle-radius", 16.0));
        amplifier = Math.max(0, plugin.getConfig().getInt("amplifier", 0));
        operation = parseOperation(plugin.getConfig().getString("operation", "ADD_SCALAR"));
    }

    public String prefixed(String key, String... kv) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String body = plugin.getConfig().getString("messages." + key, "");
        return prefix + com.frostsmp.speed.util.MessageUtil.format(body, kv);
    }

    public double magnitude() {
        return magnitude;
    }

    public AttributeModifier.Operation operation() {
        return operation;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public double particleRadius() {
        return particleRadius;
    }

    public int amplifier() {
        return amplifier;
    }

    private static AttributeModifier.Operation parseOperation(String raw) {
        if (raw == null) {
            return AttributeModifier.Operation.ADD_SCALAR;
        }
        String key = raw.trim().toUpperCase().replace('-', '_');
        if (key.equals("ADD_VALUE") || key.equals("ADD_NUMBER")) {
            return AttributeModifier.Operation.ADD_NUMBER;
        }
        if (key.equals("ADD_MULTIPLIED_BASE") || key.equals("ADD_SCALAR")) {
            return AttributeModifier.Operation.ADD_SCALAR;
        }
        if (key.equals("ADD_MULTIPLIED_TOTAL") || key.equals("MULTIPLY_SCALAR_1")) {
            return AttributeModifier.Operation.MULTIPLY_SCALAR_1;
        }
        return AttributeModifier.Operation.ADD_SCALAR;
    }
}
