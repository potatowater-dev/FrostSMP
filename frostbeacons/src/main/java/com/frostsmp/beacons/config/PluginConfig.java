package com.frostsmp.beacons.config;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class PluginConfig {

    private final JavaPlugin plugin;
    private int tickIntervalTicks;
    private int effectDurationTicks;
    private Material wandMaterial;
    private String wandName;
    private List<String> wandLore;
    private String noPermission;
    private String usage;
    private String playersOnly;
    private String reloaded;
    private String wandGiven;
    private String startSet;
    private String endSet;
    private String startMustBeBeacon;
    private String needStartFirst;
    private String endSameColumn;
    private String endAboveStart;
    private String listHeader;
    private String listEmpty;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        var config = plugin.getConfig();

        tickIntervalTicks = Math.max(5, config.getInt("tick-interval-ticks", 20));
        effectDurationTicks = Math.max(40, config.getInt("effect-duration-ticks", 180));

        String wandMatName = config.getString("wand.material", "BLAZE_ROD");
        Material parsed = Material.matchMaterial(wandMatName == null ? "BLAZE_ROD" : wandMatName);
        wandMaterial = parsed != null ? parsed : Material.BLAZE_ROD;
        wandName = config.getString("wand.name", "<gold>Beacon Wand</gold>");
        wandLore = config.getStringList("wand.lore");
        if (wandLore == null) {
            wandLore = new ArrayList<>();
        }

        noPermission = config.getString("messages.no-permission", "<red>No permission.</red>");
        usage = config.getString("messages.usage", "<gray>Usage: /frostbeacons <wand|list|reload></gray>");
        playersOnly = config.getString("messages.players-only", "<red>Players only.</red>");
        reloaded = config.getString("messages.reloaded", "<green>FrostBeacons reloaded.</green>");
        wandGiven = config.getString("messages.wand-given", "<green>Given Beacon Wand.</green>");
        startSet = config.getString("messages.start-set",
                "<green>Beam start:</green> <white>{location}</white>");
        endSet = config.getString("messages.end-set",
                "<green>Beam end:</green> <white>{location}</white>");
        startMustBeBeacon = config.getString("messages.start-must-be-beacon",
                "<red>Left-click a beacon to set the beam start.</red>");
        needStartFirst = config.getString("messages.need-start-first",
                "<yellow>Left-click a beacon first, then right-click the end block.</yellow>");
        endSameColumn = config.getString("messages.end-same-column",
                "<red>End block must be directly above the beacon (same X/Z).</red>");
        endAboveStart = config.getString("messages.end-above-start",
                "<red>End block must be above the beacon.</red>");
        listHeader = config.getString("messages.list-header", "<gold>FrostBeacons beams</gold>");
        listEmpty = config.getString("messages.list-empty", "<dark_gray>(none)</dark_gray>");
    }

    public int getTickIntervalTicks() {
        return tickIntervalTicks;
    }

    public int getEffectDurationTicks() {
        return effectDurationTicks;
    }

    public Material getWandMaterial() {
        return wandMaterial;
    }

    public String getWandName() {
        return wandName;
    }

    public List<String> getWandLore() {
        return wandLore;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getUsage() {
        return usage;
    }

    public String getPlayersOnly() {
        return playersOnly;
    }

    public String getReloaded() {
        return reloaded;
    }

    public String getWandGiven() {
        return wandGiven;
    }

    public String getStartSet() {
        return startSet;
    }

    public String getEndSet() {
        return endSet;
    }

    public String getStartMustBeBeacon() {
        return startMustBeBeacon;
    }

    public String getNeedStartFirst() {
        return needStartFirst;
    }

    public String getEndSameColumn() {
        return endSameColumn;
    }

    public String getEndAboveStart() {
        return endAboveStart;
    }

    public String getListHeader() {
        return listHeader;
    }

    public String getListEmpty() {
        return listEmpty;
    }
}
