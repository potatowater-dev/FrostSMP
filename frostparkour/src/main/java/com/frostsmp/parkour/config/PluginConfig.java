package com.frostsmp.parkour.config;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class PluginConfig {

    private final JavaPlugin plugin;

    private boolean actionbarTimerEnabled;
    private String actionbarTimerFormat;

    private String finishTitle;
    private String finishSubtitle;
    private String newBestTag;

    private boolean fallDetectionEnabled;
    private double fallYOffset;
    private boolean voidInstantTeleport;

    private boolean preventDamage;
    private boolean preventHunger;
    private boolean preventBlockBreak;
    private boolean preventBlockPlace;
    private boolean preventItemDrop;
    private boolean preventItemPickup;

    public record HotbarItemConfig(int slot, Material material, String name, List<String> lore) {
    }

    private HotbarItemConfig restartItem;
    private HotbarItemConfig checkpointItem;
    private HotbarItemConfig quitItem;

    private final Map<String, Sound> soundMap = new HashMap<>();
    private final Map<String, Float> soundVolume = new HashMap<>();
    private final Map<String, Float> soundPitch = new HashMap<>();

    private final Map<String, String> messages = new HashMap<>();

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        actionbarTimerEnabled = config.getBoolean("settings.actionbar-timer.enabled", true);
        actionbarTimerFormat = config.getString("settings.actionbar-timer.format",
                "<yellow><bold>{time}</bold></yellow> <gray>|</gray> <gold>{checkpoint}/{total_checkpoints} ⚑</gold>");

        finishTitle = config.getString("settings.finish-title.title", "<gold><bold>COURSE COMPLETE!</bold></gold>");
        finishSubtitle = config.getString("settings.finish-title.subtitle", "<gray>Your time:</gray> <yellow>{time}</yellow> {new_best}");
        newBestTag = config.getString("settings.finish-title.new-best-tag", "<green><bold>[NEW RECORD]</bold></green>");

        fallDetectionEnabled = config.getBoolean("settings.fall-detection.enabled", true);
        fallYOffset = config.getDouble("settings.fall-detection.y-offset", 4.0);
        voidInstantTeleport = config.getBoolean("settings.fall-detection.void-instant-teleport", true);

        preventDamage = config.getBoolean("settings.prevent-damage", true);
        preventHunger = config.getBoolean("settings.prevent-hunger", true);
        preventBlockBreak = config.getBoolean("settings.prevent-block-break", true);
        preventBlockPlace = config.getBoolean("settings.prevent-block-place", true);
        preventItemDrop = config.getBoolean("settings.prevent-item-drop", true);
        preventItemPickup = config.getBoolean("settings.prevent-item-pickup", true);

        restartItem = loadHotbarItem(config.getConfigurationSection("hotbar-items.restart"), 0, Material.CLOCK,
                "<gold><bold>Restart Course</bold></gold>", List.of("<gray>Right-click to teleport to start</gray>"));
        checkpointItem = loadHotbarItem(config.getConfigurationSection("hotbar-items.checkpoint"), 4, Material.ARROW,
                "<green><bold>Last Checkpoint</bold></green>", List.of("<gray>Right-click to return to checkpoint</gray>"));
        quitItem = loadHotbarItem(config.getConfigurationSection("hotbar-items.quit"), 7, Material.RED_BED,
                "<red><bold>Quit Parkour</bold></red>", List.of("<gray>Right-click to leave parkour</gray>"));

        soundMap.clear();
        soundVolume.clear();
        soundPitch.clear();
        ConfigurationSection soundsSec = config.getConfigurationSection("sounds");
        if (soundsSec != null) {
            for (String key : soundsSec.getKeys(false)) {
                String raw = soundsSec.getString(key, "");
                parseSound(key, raw);
            }
        }

        messages.clear();
        ConfigurationSection msgSec = config.getConfigurationSection("messages");
        if (msgSec != null) {
            for (String key : msgSec.getKeys(false)) {
                messages.put(key, msgSec.getString(key, ""));
            }
        }
    }

    private HotbarItemConfig loadHotbarItem(ConfigurationSection sec, int defaultSlot, Material defaultMat,
                                           String defaultName, List<String> defaultLore) {
        if (sec == null) {
            return new HotbarItemConfig(defaultSlot, defaultMat, defaultName, defaultLore);
        }
        int slot = Math.max(0, Math.min(8, sec.getInt("slot", defaultSlot)));
        Material mat = Material.matchMaterial(sec.getString("material", defaultMat.name()));
        if (mat == null) {
            mat = defaultMat;
        }
        String name = sec.getString("name", defaultName);
        List<String> lore = sec.getStringList("lore");
        if (lore.isEmpty()) {
            lore = defaultLore;
        }
        return new HotbarItemConfig(slot, mat, name, lore);
    }

    @SuppressWarnings("deprecation")
    private void parseSound(String key, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String[] parts = raw.split(":");
        try {
            Sound sound = Sound.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            float volume = parts.length > 1 ? Float.parseFloat(parts[1].trim()) : 1.0f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2].trim()) : 1.0f;
            soundMap.put(key, sound);
            soundVolume.put(key, volume);
            soundPitch.put(key, pitch);
        } catch (Exception ignored) {
        }
    }

    public Sound getSound(String key) {
        return soundMap.get(key);
    }

    public float getSoundVolume(String key) {
        return soundVolume.getOrDefault(key, 1.0f);
    }

    public float getSoundPitch(String key) {
        return soundPitch.getOrDefault(key, 1.0f);
    }

    public String msg(String key) {
        String prefix = messages.getOrDefault("prefix", "");
        String raw = messages.get(key);
        if (raw == null) {
            return prefix + "<red>Message key '" + key + "' missing!</red>";
        }
        return prefix + raw;
    }

    public String rawMsg(String key) {
        return messages.getOrDefault(key, "");
    }

    public boolean isActionbarTimerEnabled() {
        return actionbarTimerEnabled;
    }

    public String getActionbarTimerFormat() {
        return actionbarTimerFormat;
    }

    public String getFinishTitle() {
        return finishTitle;
    }

    public String getFinishSubtitle() {
        return finishSubtitle;
    }

    public String getNewBestTag() {
        return newBestTag;
    }

    public boolean isFallDetectionEnabled() {
        return fallDetectionEnabled;
    }

    public double getFallYOffset() {
        return fallYOffset;
    }

    public boolean isVoidInstantTeleport() {
        return voidInstantTeleport;
    }

    public boolean isPreventDamage() {
        return preventDamage;
    }

    public boolean isPreventHunger() {
        return preventHunger;
    }

    public boolean isPreventBlockBreak() {
        return preventBlockBreak;
    }

    public boolean isPreventBlockPlace() {
        return preventBlockPlace;
    }

    public boolean isPreventItemDrop() {
        return preventItemDrop;
    }

    public boolean isPreventItemPickup() {
        return preventItemPickup;
    }

    public HotbarItemConfig getRestartItem() {
        return restartItem;
    }

    public HotbarItemConfig getCheckpointItem() {
        return checkpointItem;
    }

    public HotbarItemConfig getQuitItem() {
        return quitItem;
    }
}
