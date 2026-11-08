package com.frostsmp.clear.config;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PluginConfig {

    private final JavaPlugin plugin;

    private String warningMessage;
    private String noPermission;
    private String usage;
    private String reloaded;
    private TaskSettings items;
    private TaskSettings blocks;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        var config = plugin.getConfig();

        warningMessage = config.getString("warning-message", "<red><bold>{label} clears in {time}</bold></red>");
        noPermission = config.getString("messages.no-permission", "<red>No permission.</red>");
        usage = config.getString("messages.usage", "<gray>Usage: /frostclear reload</gray>");
        reloaded = config.getString("messages.reloaded", "<green>FrostClear reloaded.</green>");

        items = loadTask("items", true, 300, "kill @e[type=item]", "Items",
                List.of(30, 10, 5), "BLOCK_NOTE_BLOCK_PLING", 1.0, 1.2);
        blocks = loadTask("blocks", true, 600, "kit cleanup", "Blocks",
                List.of(10, 5, 1), "BLOCK_NOTE_BLOCK_BASS", 1.0, 0.8);
    }

    private TaskSettings loadTask(
            String path,
            boolean defaultEnabled,
            int defaultInterval,
            String defaultCommand,
            String defaultLabel,
            List<Integer> defaultWarnings,
            String defaultSound,
            double defaultVolume,
            double defaultPitch
    ) {
        var config = plugin.getConfig();
        boolean enabled = config.getBoolean(path + ".enabled", defaultEnabled);
        int intervalSeconds = Math.max(1, config.getInt(path + ".interval-seconds", defaultInterval));
        String command = stripSlash(config.getString(path + ".command", defaultCommand));
        String label = config.getString(path + ".label", defaultLabel);
        if (label == null || label.isBlank()) {
            label = defaultLabel;
        }

        List<Integer> rawWarnings = config.contains(path + ".warnings")
                ? config.getIntegerList(path + ".warnings")
                : defaultWarnings;
        List<Integer> warnings = new ArrayList<>();
        for (Integer warning : rawWarnings) {
            if (warning != null && warning > 0 && warning < intervalSeconds && !warnings.contains(warning)) {
                warnings.add(warning);
            }
        }

        boolean soundEnabled = config.getBoolean(path + ".sound.enabled", true);
        Sound sound = parseSound(config.getString(path + ".sound.name"), defaultSound);
        float volume = (float) config.getDouble(path + ".sound.volume", defaultVolume);
        float pitch = (float) config.getDouble(path + ".sound.pitch", defaultPitch);

        return new TaskSettings(enabled, intervalSeconds, command, label, warnings, soundEnabled, sound, volume, pitch);
    }

    private static String stripSlash(String command) {
        if (command == null) {
            return "";
        }
        String trimmed = command.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1).trim();
        }
        return trimmed;
    }

    private Sound parseSound(String name, String fallbackName) {
        Sound sound = resolveSound(name);
        if (sound != null) {
            return sound;
        }
        if (name != null && !name.isBlank()) {
            plugin.getLogger().warning("Invalid sound: " + name);
        }
        Sound fallback = resolveSound(fallbackName);
        if (fallback == null) {
            throw new IllegalStateException("Unknown fallback sound: " + fallbackName);
        }
        return fallback;
    }

    private static Sound resolveSound(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String lower = name.trim().toLowerCase(Locale.ROOT);
        String namespaced = lower.contains(":") ? lower : "minecraft:" + lower;
        NamespacedKey key = NamespacedKey.fromString(namespaced);
        if (key != null) {
            Sound byKey = Registry.SOUNDS.get(key);
            if (byKey != null) {
                return byKey;
            }
        }
        String enumForm = lower.replace(':', '_').replace('.', '_');
        for (Sound sound : Registry.SOUNDS) {
            NamespacedKey soundKey = Registry.SOUNDS.getKey(sound);
            if (soundKey == null) {
                continue;
            }
            String asEnum = soundKey.getKey().replace('.', '_').replace('/', '_');
            if (asEnum.equals(enumForm) || (soundKey.getNamespace() + "_" + asEnum).equals(enumForm)) {
                return sound;
            }
        }
        return null;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getUsage() {
        return usage;
    }

    public String getReloaded() {
        return reloaded;
    }

    public TaskSettings getItems() {
        return items;
    }

    public TaskSettings getBlocks() {
        return blocks;
    }

    public static final class TaskSettings {

        private final boolean enabled;
        private final int intervalSeconds;
        private final String command;
        private final String label;
        private final List<Integer> warnings;
        private final boolean soundEnabled;
        private final Sound sound;
        private final float volume;
        private final float pitch;

        private TaskSettings(
                boolean enabled,
                int intervalSeconds,
                String command,
                String label,
                List<Integer> warnings,
                boolean soundEnabled,
                Sound sound,
                float volume,
                float pitch
        ) {
            this.enabled = enabled;
            this.intervalSeconds = intervalSeconds;
            this.command = command;
            this.label = label;
            this.warnings = List.copyOf(warnings);
            this.soundEnabled = soundEnabled;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getIntervalSeconds() {
            return intervalSeconds;
        }

        public String getCommand() {
            return command;
        }

        public String getLabel() {
            return label;
        }

        public List<Integer> getWarnings() {
            return warnings;
        }

        public boolean isSoundEnabled() {
            return soundEnabled;
        }

        public Sound getSound() {
            return sound;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }
    }
}
