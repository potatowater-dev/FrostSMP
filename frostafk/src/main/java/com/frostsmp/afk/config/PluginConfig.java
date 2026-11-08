package com.frostsmp.afk.config;

import com.frostsmp.afk.FrostAfkPlugin;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.List;

public final class PluginConfig {

    private final FrostAfkPlugin plugin;

    private int teleportDelaySeconds;
    private long rewardIntervalMs;
    private String crateId;
    private int keysPerReward;
    private int tickIntervalTicks;
    private Material wandMaterial;
    private String wandName;
    private List<String> wandLore;
    private BarColor bossBarColor;
    private BarStyle bossBarStyle;
    private String bossBarTitle;
    private String noPermission;
    private String playersOnly;
    private String usage;
    private String reloaded;
    private String wandGiven;
    private String pos1Set;
    private String pos2Set;
    private String spawnSet;
    private String regionIncomplete;
    private String spawnMissing;
    private String afkStart;
    private String afkCancelledMove;
    private String afkCancelled;
    private String afkAlready;
    private String afkTeleported;
    private String reward;
    private String cratesMissing;
    private String infoHeader;
    private String infoRegion;
    private String infoSpawn;
    private String infoNone;

    public PluginConfig(FrostAfkPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        var config = plugin.getConfig();

        teleportDelaySeconds = Math.max(0, config.getInt("teleport-delay-seconds", 5));
        rewardIntervalMs = Math.max(1000L, config.getLong("reward-interval-ms", 1_800_000L));
        crateId = config.getString("crate-id", "afk");
        if (crateId != null) {
            crateId = crateId.toLowerCase();
        }
        keysPerReward = Math.max(1, config.getInt("keys-per-reward", 1));
        tickIntervalTicks = Math.max(1, config.getInt("tick-interval-ticks", 20));

        wandMaterial = Material.matchMaterial(config.getString("wand.material", "BLAZE_ROD"));
        if (wandMaterial == null) {
            wandMaterial = Material.BLAZE_ROD;
        }
        wandName = config.getString("wand.name", "<gold>AFK Wand</gold>");
        wandLore = config.getStringList("wand.lore");

        bossBarColor = parseBarColor(config.getString("bossbar.color", "BLUE"));
        bossBarStyle = parseBarStyle(config.getString("bossbar.style", "SOLID"));
        bossBarTitle = config.getString("bossbar.title", "<aqua>AFK Reward</aqua> <gray>·</gray> <white>{remaining}</white>");

        noPermission = config.getString("messages.no-permission", "<red>No permission.</red>");
        playersOnly = config.getString("messages.players-only", "<red>Players only.</red>");
        usage = config.getString("messages.usage", "<gray>Usage: /frostafk <wand|pos1|pos2|setspawn|info|reload></gray>");
        reloaded = config.getString("messages.reloaded", "<green>FrostAFK reloaded.</green>");
        wandGiven = config.getString("messages.wand-given", "<green>Given AFK Wand.</green>");
        pos1Set = config.getString("messages.pos1-set", "<green>AFK pos1:</green> <white>{location}</white>");
        pos2Set = config.getString("messages.pos2-set", "<green>AFK pos2:</green> <white>{location}</white>");
        spawnSet = config.getString("messages.spawn-set", "<green>AFK spawn:</green> <white>{location}</white>");
        regionIncomplete = config.getString("messages.region-incomplete", "<red>AFK region is not fully set.</red>");
        spawnMissing = config.getString("messages.spawn-missing", "<red>AFK spawn is not set.</red>");
        afkStart = config.getString("messages.afk-start", "<gray>Teleporting to AFK in</gray> <white>{seconds}s</white><gray>...</gray>");
        afkCancelledMove = config.getString("messages.afk-cancelled-move", "<red>AFK teleport cancelled — you moved.</red>");
        afkCancelled = config.getString("messages.afk-cancelled", "<yellow>AFK teleport cancelled.</yellow>");
        afkAlready = config.getString("messages.afk-already", "<yellow>You are already teleporting to AFK.</yellow>");
        afkTeleported = config.getString("messages.afk-teleported", "<green>Welcome to the AFK zone.</green>");
        reward = config.getString("messages.reward", "<green>AFK reward:</green> <white>{amount}x {crate}</white> <green>key!</green>");
        cratesMissing = config.getString("messages.crates-missing", "<red>FrostCrates is not available.</red>");
        infoHeader = config.getString("messages.info-header", "<gold>FrostAFK</gold>");
        infoRegion = config.getString("messages.info-region", "<gray>Region:</gray> <white>{region}</white>");
        infoSpawn = config.getString("messages.info-spawn", "<gray>Spawn:</gray> <white>{spawn}</white>");
        infoNone = config.getString("messages.info-none", "<dark_gray>(not set)</dark_gray>");
    }

    private static BarColor parseBarColor(String raw) {
        try {
            return BarColor.valueOf(raw.toUpperCase());
        } catch (Exception ignored) {
            return BarColor.BLUE;
        }
    }

    private static BarStyle parseBarStyle(String raw) {
        try {
            return BarStyle.valueOf(raw.toUpperCase());
        } catch (Exception ignored) {
            return BarStyle.SOLID;
        }
    }

    public int getTeleportDelaySeconds() {
        return teleportDelaySeconds;
    }

    public long getRewardIntervalMs() {
        return rewardIntervalMs;
    }

    public String getCrateId() {
        return crateId;
    }

    public int getKeysPerReward() {
        return keysPerReward;
    }

    public int getTickIntervalTicks() {
        return tickIntervalTicks;
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

    public BarColor getBossBarColor() {
        return bossBarColor;
    }

    public BarStyle getBossBarStyle() {
        return bossBarStyle;
    }

    public String getBossBarTitle() {
        return bossBarTitle;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getPlayersOnly() {
        return playersOnly;
    }

    public String getUsage() {
        return usage;
    }

    public String getReloaded() {
        return reloaded;
    }

    public String getWandGiven() {
        return wandGiven;
    }

    public String getPos1Set() {
        return pos1Set;
    }

    public String getPos2Set() {
        return pos2Set;
    }

    public String getSpawnSet() {
        return spawnSet;
    }

    public String getRegionIncomplete() {
        return regionIncomplete;
    }

    public String getSpawnMissing() {
        return spawnMissing;
    }

    public String getAfkStart() {
        return afkStart;
    }

    public String getAfkCancelledMove() {
        return afkCancelledMove;
    }

    public String getAfkCancelled() {
        return afkCancelled;
    }

    public String getAfkAlready() {
        return afkAlready;
    }

    public String getAfkTeleported() {
        return afkTeleported;
    }

    public String getReward() {
        return reward;
    }

    public String getCratesMissing() {
        return cratesMissing;
    }

    public String getInfoHeader() {
        return infoHeader;
    }

    public String getInfoRegion() {
        return infoRegion;
    }

    public String getInfoSpawn() {
        return infoSpawn;
    }

    public String getInfoNone() {
        return infoNone;
    }
}
